package dev.elian.lab.batch.jobs;

import dev.elian.lab.batch.SendOps;
import dev.elian.lab.batch.SendTarget;
import dev.elian.lab.common.Knobs;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ② chunk 로 전환한 상태.
 *
 * <p>chunk 하나가 트랜잭션 하나다. Tasklet 보다 트랜잭션이 훨씬 짧아진다.
 * <b>그런데 운영에서는 여기서도 데드락이 났다.</b> 그게 이 실험의 핵심 관찰이다.
 *
 * <p>reader 는 chunk 트랜잭션 <b>안에서</b> 호출된다 (Spring Batch 기본 동작).
 * {@code queryInTx=false} 면 트랜잭션 밖에서 미리 조회해 둔 목록을 읽는다.
 */
@Component
public class ChunkSendJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final SendOps ops;

    public ChunkSendJob(JobRepository jobRepository, PlatformTransactionManager txManager, SendOps ops) {
        this.jobRepository = jobRepository;
        this.txManager = txManager;
        this.ops = ops;
    }

    public Job build(Knobs k, long publishLatencyMicros, int totalLimit, dev.elian.lab.batch.TxTimer timer) {
        ItemReader<SendTarget> reader = k.queryInTx()
                ? new PendingReader(ops, k.chunkSize())      // 조회가 chunk 트랜잭션 안
                : new ListItemReader<>(ops.findPending(totalLimit));  // 조회를 밖에서 미리

        ItemWriter<SendTarget> writer = chunk -> {
            List<SendTarget> items = new ArrayList<>(chunk.getItems());
            long inTx = k.publishInTx() ? publishLatencyMicros : 0;
            if (!k.publishInTx()) SendOps.publishOutside(publishLatencyMicros);
            ops.send(items, k, inTx);
        };

        Step step = new StepBuilder("chunkSend-" + k.id(), jobRepository)
                .<SendTarget, SendTarget>chunk(k.chunkSize(), txManager)
                .reader(reader)
                .writer(writer)
                .transactionAttribute(TxAttr.of(k))   // 격리수준을 chunk 트랜잭션에 적용
                .listener(new ChunkListener() {
                    @Override public void beforeChunk(ChunkContext c) { timer.begin(); }
                    @Override public void afterChunk(ChunkContext c) { timer.end(); }
                    @Override public void afterChunkError(ChunkContext c) { timer.end(); }
                })
                .build();

        return new JobBuilder("chunkSendJob-" + k.id(), jobRepository).start(step).build();
    }

    /** chunk 트랜잭션 안에서 PENDING 을 페이지 단위로 읽는다. */
    static class PendingReader implements ItemReader<SendTarget> {
        private final SendOps ops;
        private final int pageSize;
        private Iterator<SendTarget> current = List.<SendTarget>of().iterator();

        PendingReader(SendOps ops, int pageSize) { this.ops = ops; this.pageSize = pageSize; }

        @Override
        public SendTarget read() {
            if (!current.hasNext()) {
                List<SendTarget> page = ops.findPending(pageSize);   // 트랜잭션 안에서 실행된다
                if (page.isEmpty()) return null;
                current = page.iterator();
            }
            return current.next();
        }
    }
}
