package com.dolpin.global.batch.job;

import com.dolpin.global.batch.dto.MomentData;
import com.dolpin.global.batch.dto.PopularMomentResult;
import com.dolpin.global.batch.processor.PopularityCalculationProcessor;
import com.dolpin.global.batch.writer.PopularMomentRedisWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PopularMomentBatchJobConfig {

    private final DataSource dataSource;
    private final PopularityCalculationProcessor popularityProcessor;
    private final PopularMomentRedisWriter popularMomentWriter;

    /**
     * 인기 게시글 집계 Job (유일한 배치 Job)
     */
    @Bean
    public Job popularMomentJob(JobRepository jobRepository,
                                Step dailyPopularMomentStep,
                                Step weeklyPopularMomentStep) {
        return new JobBuilder("popularMomentJob", jobRepository)
                .start(dailyPopularMomentStep)
                .next(weeklyPopularMomentStep)
                .build();
    }

    /**
     * 일간 인기 게시글 집계 Step
     */
    @Bean
    public Step dailyPopularMomentStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager) {
        return new StepBuilder("dailyPopularMomentStep", jobRepository)
                .<MomentData, PopularMomentResult>chunk(50, transactionManager)
                .reader(dailyMomentReader(null))
                .processor(popularityProcessor)
                .writer(popularMomentWriter)
                .build();
    }

    /**
     * 주간 인기 게시글 집계 Step
     */
    @Bean
    public Step weeklyPopularMomentStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager) {
        return new StepBuilder("weeklyPopularMomentStep", jobRepository)
                .<MomentData, PopularMomentResult>chunk(100, transactionManager)
                .reader(weeklyMomentReader(null))
                .processor(popularityProcessor)
                .writer(popularMomentWriter)
                .build();
    }

    /**
     * 일간 데이터 Reader - 어제 생성된 공개 게시글
     */
    @Bean
    @StepScope
    public ItemReader<MomentData> dailyMomentReader(
            @Value("#{jobParameters[targetDate]}") String targetDate) {

        LocalDateTime date = targetDate != null ?
                LocalDateTime.parse(targetDate) : LocalDateTime.now().minusDays(1);
        LocalDateTime dayStart = date.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime dayEnd = dayStart.plusDays(1);

        log.info("Reading daily moments from {} to {}", dayStart, dayEnd);

        return new JdbcCursorItemReaderBuilder<MomentData>()
                .name("dailyMomentReader")
                .dataSource(dataSource)
                .sql("""
                    SELECT 
                        m.id,
                        m.title,
                        m.content,
                        m.thumbnail_url,
                        m.user_id,
                        m.created_at,
                        m.view_count,
                        u.username as author_name,
                        u.image_url as author_image,
                        COALESCE(c.comment_count, 0) as comment_count,
                        'daily' as period_type
                    FROM moments m
                    JOIN users u ON m.user_id = u.id
                    LEFT JOIN (
                        SELECT moment_id, COUNT(*) as comment_count
                        FROM comments 
                        WHERE deleted_at IS NULL
                        GROUP BY moment_id
                    ) c ON m.id = c.moment_id
                    WHERE m.created_at >= ? 
                    AND m.created_at < ?
                    AND m.is_public = true
                    AND m.deleted_at IS NULL
                    ORDER BY m.id
                    """)
                .preparedStatementSetter((ps) -> {
                    ps.setObject(1, dayStart);
                    ps.setObject(2, dayEnd);
                })
                .rowMapper(new BeanPropertyRowMapper<>(MomentData.class))
                .build();
    }

    /**
     * 주간 데이터 Reader - 지난 주 게시글
     */
    @Bean
    @StepScope
    public ItemReader<MomentData> weeklyMomentReader(
            @Value("#{jobParameters[targetDate]}") String targetDate) {

        LocalDateTime date = targetDate != null ?
                LocalDateTime.parse(targetDate) : LocalDateTime.now();
        LocalDateTime weekStart = date.minusDays(7).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime weekEnd = date.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        log.info("Reading weekly moments from {} to {}", weekStart, weekEnd);

        return new JdbcCursorItemReaderBuilder<MomentData>()
                .name("weeklyMomentReader")
                .dataSource(dataSource)
                .sql("""
                    SELECT 
                        m.id,
                        m.title,
                        m.content,
                        m.thumbnail_url,
                        m.user_id,
                        m.created_at,
                        m.view_count,
                        u.username as author_name,
                        u.image_url as author_image,
                        COALESCE(c.comment_count, 0) as comment_count,
                        'weekly' as period_type
                    FROM moments m
                    JOIN users u ON m.user_id = u.id
                    LEFT JOIN (
                        SELECT moment_id, COUNT(*) as comment_count
                        FROM comments 
                        WHERE deleted_at IS NULL
                        GROUP BY moment_id
                    ) c ON m.id = c.moment_id
                    WHERE m.created_at >= ? 
                    AND m.created_at <= ?
                    AND m.is_public = true
                    AND m.deleted_at IS NULL
                    ORDER BY m.id
                    """)
                .preparedStatementSetter((ps) -> {
                    ps.setObject(1, weekStart);
                    ps.setObject(2, weekEnd);
                })
                .rowMapper(new BeanPropertyRowMapper<>(MomentData.class))
                .build();
    }
}
