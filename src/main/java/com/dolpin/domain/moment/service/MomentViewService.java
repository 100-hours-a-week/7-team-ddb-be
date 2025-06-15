package com.dolpin.domain.moment.service;

import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
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

    /**
     * 조회수 증가 (동시성 보장)
     * - 모든 조회에 대해 증가 (자신의 기록 포함)
     * - 원자적 연산으로 동시성 문제 해결
     */
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void incrementViewCount(Long momentId) {
        // 기록 존재 여부만 확인
        boolean exists = momentRepository.existsById(momentId);
        if (!exists) {
            throw new BusinessException(ResponseStatus.USER_NOT_FOUND.withMessage("기록을 찾을 수 없습니다."));
        }

        // 조회수 증가 (모든 조회에 대해)
        int updatedRows = momentRepository.incrementViewCount(momentId);

        if (updatedRows > 0) {
            log.debug("View count incremented - momentId: {}", momentId);
        } else {
            log.warn("Failed to increment view count - momentId: {}", momentId);
        }
    }

    /**
     * 특정 기록의 현재 조회수 조회
     */
    @Transactional(readOnly = true)
    public Long getViewCount(Long momentId) {
        return momentRepository.findBasicMomentById(momentId)
                .map(Moment::getViewCount)
                .orElse(0L);
    }
}
