package com.dolpin.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchJobScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("popularMomentJob")
    private final Job popularMomentJob;

    /**
     * 매일 오전 6시 - 인기 게시글 집계 배치 실행
     */
    @Scheduled(cron = "0 0 21 * * *") // UTC 21:00 = 한국시간 06:00
    public void runPopularMomentBatch() {
        try {
            log.info("Starting popular moment batch job");

            JobParameters jobParameters = createJobParameters();
            JobExecution jobExecution = jobLauncher.run(popularMomentJob, jobParameters);

            log.info("Popular moment batch job launched successfully with execution id: {}",
                    jobExecution.getId());

        } catch (JobExecutionAlreadyRunningException e) {
            handleAlreadyRunningException(e);
        } catch (JobInstanceAlreadyCompleteException e) {
            handleAlreadyCompleteException(e);
        } catch (JobRestartException e) {
            handleRestartException(e);
            // 재시도 로직은 별도로 구현하거나 수동 개입 필요
        } catch (JobParametersInvalidException e) {
            handleInvalidParametersException(e);
            // 파라미터 오류는 즉시 수정이 어려우므로 재시도하지 않음
        } catch (Exception e) {
            handleUnexpectedException(e);
            throw e;
        }
    }

    /**
     * 수동 배치 실행 (관리자용)
     */
    public BatchExecutionResult manualRunPopularMomentBatch() {
        log.info("Manual popular moment batch execution requested");

        try {
            JobParameters jobParameters = createJobParameters();
            JobExecution jobExecution = jobLauncher.run(popularMomentJob, jobParameters);

            return BatchExecutionResult.success(jobExecution.getId(), jobExecution.getStatus());

        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("배치 작업이 이미 실행 중입니다: {}", e.getMessage());
            return BatchExecutionResult.alreadyRunning(e.getMessage());

        } catch (JobInstanceAlreadyCompleteException e) {
            log.warn("배치 작업이 이미 완료되었습니다: {}", e.getMessage());
            return BatchExecutionResult.alreadyComplete(e.getMessage());

        } catch (JobRestartException e) {
            log.error("배치 작업 재시작 실패: {}", e.getMessage(), e);
            return BatchExecutionResult.restartFailed(e.getMessage());

        } catch (JobParametersInvalidException e) {
            log.error("잘못된 배치 파라미터: {}", e.getMessage(), e);
            return BatchExecutionResult.invalidParameters(e.getMessage());

        } catch (Exception e) {
            log.error("예상치 못한 배치 실행 오류", e);
            return BatchExecutionResult.unexpectedError(e.getMessage());
        }
    }

    /**
     * Job Parameters 생성 - 재사용성을 위해 분리
     */
    private JobParameters createJobParameters() {
        return new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("targetDate", LocalDateTime.now().minusDays(1).toString())
                .addString("executionId", java.util.UUID.randomUUID().toString()) // 중복 실행 방지
                .toJobParameters();
    }

    /**
     * 이미 실행 중인 작업 예외 처리
     */
    private void handleAlreadyRunningException(JobExecutionAlreadyRunningException e) {
        log.warn("Popular moment batch job is already running: {}", e.getMessage());
        // 알림 서비스나 모니터링 시스템에 상태 전송 가능
    }

    /**
     * 이미 완료된 작업 예외 처리
     */
    private void handleAlreadyCompleteException(JobInstanceAlreadyCompleteException e) {
        log.info("Popular moment batch job already completed for this parameter set: {}", e.getMessage());
        // 정상적인 상황으로 간주
    }

    /**
     * 재시작 실패 예외 처리
     */
    private void handleRestartException(JobRestartException e) {
        log.error("Failed to restart popular moment batch job: {}", e.getMessage(), e);
        // 수동 개입이 필요한 상황
    }

    /**
     * 잘못된 파라미터 예외 처리
     */
    private void handleInvalidParametersException(JobParametersInvalidException e) {
        log.error("Invalid job parameters for popular moment batch: {}", e.getMessage(), e);
        // 파라미터 검증 로직 개선 필요
    }

    /**
     * 예상치 못한 예외 처리
     */
    private void handleUnexpectedException(Exception e) {
        log.error("Unexpected error occurred during popular moment batch execution", e);
        // 긴급 알림 필요
    }

    /**
     * 배치 실행 결과를 나타내는 클래스
     */
    public static class BatchExecutionResult {
        private final boolean success;
        private final String message;
        private final Long executionId;
        private final BatchStatus status;
        private final ResultType resultType;

        private BatchExecutionResult(boolean success, String message, Long executionId,
                                     BatchStatus status, ResultType resultType) {
            this.success = success;
            this.message = message;
            this.executionId = executionId;
            this.status = status;
            this.resultType = resultType;
        }

        public static BatchExecutionResult success(Long executionId, BatchStatus status) {
            return new BatchExecutionResult(true, "배치 작업이 성공적으로 시작되었습니다.",
                    executionId, status, ResultType.SUCCESS);
        }

        public static BatchExecutionResult alreadyRunning(String message) {
            return new BatchExecutionResult(false, message, null, null, ResultType.ALREADY_RUNNING);
        }

        public static BatchExecutionResult alreadyComplete(String message) {
            return new BatchExecutionResult(true, message, null, null, ResultType.ALREADY_COMPLETE);
        }

        public static BatchExecutionResult restartFailed(String message) {
            return new BatchExecutionResult(false, message, null, null, ResultType.RESTART_FAILED);
        }

        public static BatchExecutionResult invalidParameters(String message) {
            return new BatchExecutionResult(false, message, null, null, ResultType.INVALID_PARAMETERS);
        }

        public static BatchExecutionResult unexpectedError(String message) {
            return new BatchExecutionResult(false, message, null, null, ResultType.UNEXPECTED_ERROR);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Long getExecutionId() { return executionId; }
        public BatchStatus getStatus() { return status; }
        public ResultType getResultType() { return resultType; }

        public enum ResultType {
            SUCCESS, ALREADY_RUNNING, ALREADY_COMPLETE,
            RESTART_FAILED, INVALID_PARAMETERS, UNEXPECTED_ERROR
        }
    }
}
