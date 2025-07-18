package com.dolpin.global.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicatePreventionService {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "duplicate_request:";
    private static final String CONTENT_PREFIX = "duplicate_content:";

    public <T> T executeWithLock(String key, int waitTime, int leaseTime, LockAction<T> action) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);

        try {
            // tryLock(waitTime, leaseTime, TimeUnit) - 대기시간, 점유시간 설정
            boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);

            if (!acquired) {
                log.warn("중복 요청 감지 - 락 획득 실패: key={}", key);
                throw new RuntimeException("동일한 요청이 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            log.debug("락 획득 성공: key={}", key);
            return action.execute();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("락 대기 중 인터럽트 발생: key={}", key, e);
            throw new RuntimeException("요청 처리 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("락 실행 중 오류: key={}", key, e);
            throw new RuntimeException("요청 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        } finally {
            // 락이 현재 스레드에 의해 점유되어 있을 때만 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("락 해제 완료: key={}", key);
            }
        }
    }

    public <T> T executeWithLock(String key, LockAction<T> action) {
        return executeWithLock(key, 0, 3, action);
    }

    public String generateKey(Long userId, String action) {
        return String.format("%d:%s", userId, action);
    }

    public String generateKey(Long userId, String action, Long resourceId) {
        return String.format("%d:%s:%d", userId, action, resourceId);
    }

    public String generateContentKey(Long userId, Long resourceId, String content) {
        String contentHash = generateContentHash(content);
        return String.format("content:%d:%d:%s", userId, resourceId, contentHash);
    }

    private String generateContentHash(String content) {
        return String.valueOf(Math.abs(content.trim().hashCode()));
    }

    public void checkDuplicateContent(String contentKey, String errorMessage, Duration blockDuration) {
        RBucket<Boolean> bucket = redissonClient.getBucket(CONTENT_PREFIX + contentKey);

        if (bucket.isExists()) {
            log.warn("중복 내용 감지: key={}", contentKey);
            throw new IllegalArgumentException(errorMessage);
        }

        // 중복 방지 마킹 (TTL 설정)
        bucket.set(true, blockDuration);
        log.debug("내용 중복 방지 마킹: key={}, duration={}", contentKey, blockDuration);
    }

    @FunctionalInterface
    public interface LockAction<T> {
        T execute();
    }
}
