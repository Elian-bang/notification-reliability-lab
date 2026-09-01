package dev.elian.lab.batch.jobs;

import dev.elian.lab.batch.SendOps;
import dev.elian.lab.batch.SendTarget;
import dev.elian.lab.common.Knobs;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * ① 운영 최초 상태 — Tasklet.
 *
 * <p>Tasklet 하나가 트랜잭션 하나다. 조회부터 전 건 발송까지 <b>한 트랜잭션</b>에서 끝낸다.
 * 그래서 락을 가장 오래 쥔다.
 */
@Component
public class TaskletSendJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final SendOps ops;

    public TaskletSendJob(JobRepository jobRepository, PlatformTransactionManager txManager, SendOps ops) {
        this.jobRepository = jobRepository;
        this.txManager = txManager;
        this.ops = ops;
    }

    public Job build(Knobs k, long apiDelayMicros, int batchSize) {
        Step step = new StepBuilder("taskletSend-" + k.id(), jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    List<SendTarget> targets = ops.findPending(batchSize);
                    if (targets.isEmpty()) return RepeatStatus.FINISHED;
                    long inTx = k.apiCallInTx() ? apiDelayMicros : 0;
                    if (!k.apiCallInTx()) SendOps.waitExternalApi(apiDelayMicros);
                    ops.send(targets, k, inTx);
                    contribution.incrementWriteCount(targets.size());
                    return RepeatStatus.CONTINUABLE;   // 남은 게 없을 때까지 반복
                }, txManager)
                .build();

        return new JobBuilder("taskletSendJob-" + k.id(), jobRepository).start(step).build();
    }
}
