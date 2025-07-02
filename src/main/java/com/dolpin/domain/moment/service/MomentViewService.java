package com.dolpin.domain.moment.service;

import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.moment.service.cache.MomentCacheService;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentViewService {

    private final MomentRepository momentRepository;
    private final MomentCacheService momentCacheService; // 추가

    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void incrementViewCount(Long momentId) {
        // 기록 존재 여부만 확인
        boolean exists = momentRepository.existsById(momentId);
        if (!exists) {
            throw new BusinessException(ResponseStatus.USER_NOT_FOUND.withMessage("기록을 찾을 수 없습니다."));
        }

        // DB에서 조회수 증가
        int updatedRows = momentRepository.incrementViewCount(momentId);

        if (updatedRows > 0) {
            // 캐시된 조회수도 함께 증가
            updateViewCountCache(momentId);

            log.debug("View count incremented - momentId: {}", momentId);
        } else {
            log.warn("Failed to increment view count - momentId: {}", momentId);
        }
    }

    // 캐시된 조회수 실시간 업데이트
    private void updateViewCountCache(Long momentId) {
        try {
            // 현재 캐시된 조회수 확인
            Long cachedViewCount = momentCacheService.getViewCount(momentId);

            if (cachedViewCount != null) {
                // 캐시된 값이 있으면 +1 증가
                momentCacheService.cacheViewCount(momentId, cachedViewCount + 1);
                log.debug("캐시된 조회수 업데이트: momentId={}, newCount={}", momentId, cachedViewCount + 1);
            } else {
                // 캐시된 값이 없으면 DB에서 최신 값 조회 후 캐싱
                Long currentViewCount = getCurrentViewCount(momentId);
                momentCacheService.cacheViewCount(momentId, currentViewCount);
                log.debug("조회수 신규 캐싱: momentId={}, count={}", momentId, currentViewCount);
            }
        } catch (Exception e) {
            log.warn("조회수 캐시 업데이트 실패: momentId={}", momentId, e);
            // 캐시 실패해도 DB 업데이트는 성공했으므로 진행
        }
    }

    @Transactional(readOnly = true)
    public Long getViewCount(Long momentId) {
        return momentRepository.findBasicMomentById(momentId)
                .map(Moment::getViewCount)
                .orElse(0L);
    }

    // DB에서 현재 조회수 조회

    private Long getCurrentViewCount(Long momentId) {
        return momentRepository.findBasicMomentById(momentId)
                .map(Moment::getViewCount)
                .orElse(0L);
    }
}
