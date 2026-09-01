package dev.elian.lab.batch;

import dev.elian.lab.common.Variant;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.stereotype.Component;


import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * variant 마다 Job 을 만든다.
 *
 * <p><b>핵심</b>: chunk-oriented step 은 reader → processor → writer 한 사이클이 한 트랜잭션이다.
 * 즉 <b>대상 조회가 트랜잭션 안에 있는 것이 Spring Batch 의 기본 동작</b>이고,
 * 그게 운영에서 겪은 원본 조건이다.
 *
 * <p>V1(조회를 트랜잭션 밖으로)은 이 표준 구조를 벗어나야 만들 수 있다 —
 * 선행 조회로 대상을 확보하고 chunk 에서는 쓰기만 한다.
 */
@Component
public class JobFactory {

    private final JobRepository jobRepository;
    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager txManager;

    public JobFactory(JobRepository jobRepository, DataSource dataSource, JdbcTemplate jdbc,
                      PlatformTransactionManager txManager) {
        this.jobRepository = jobRepository;
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        this.txManager = txManager;
    }

    public Job build(Variant v, long apiDelayMicros, int limit) {
        Step step = new StepBuilder("sendStep-" + v.id(), jobRepository)
                .<SendTarget, SendTarget>chunk(v.chunkSize(), txManager)   // JobRepository 와 같은 TM (JPA)
                .reader(reader(v, limit))
                .processor((ItemProcessor<SendTarget, SendTarget>) t -> t)
                .writer(new SendItemWriter(jdbc, v, apiDelayMicros))
                .faultTolerant()
                .build();

        return new JobBuilder("sendJob-" + v.id(), jobRepository).start(step).build();
    }

    private ItemReader<SendTarget> reader(Variant v, int limit) {
        if (v.queryInsideTx()) {
            // 표준 동작 — 페이지 조회가 chunk 트랜잭션 안에서 실행된다
            JdbcPagingItemReader<SendTarget> r = new JdbcPagingItemReader<>();
            r.setDataSource(dataSource);
            r.setPageSize(v.chunkSize());
            r.setFetchSize(v.chunkSize());
            MySqlPagingQueryProvider q = new MySqlPagingQueryProvider();
            q.setSelectClause("SELECT id, account_id");
            q.setFromClause("FROM notification_request");
            q.setWhereClause("WHERE status = 'PENDING'");
            Map<String, Order> sort = new LinkedHashMap<>();
            sort.put("id", Order.ASCENDING);
            q.setSortKeys(sort);
            r.setQueryProvider(q);
            r.setRowMapper((rs, i) -> new SendTarget(rs.getLong("id"), rs.getLong("account_id")));
            r.setSaveState(false);
            try { r.afterPropertiesSet(); } catch (Exception e) { throw new IllegalStateException(e); }
            return r;
        }

        // V1 — 조회를 트랜잭션 밖에서 선행 수행하고, chunk 에서는 쓰기만 한다
        List<SendTarget> targets = jdbc.query(
                "SELECT id, account_id FROM notification_request WHERE status = 'PENDING' ORDER BY id LIMIT ?",
                (rs, i) -> new SendTarget(rs.getLong("id"), rs.getLong("account_id")), limit);
        return new ListItemReader<>(targets);
    }
}
