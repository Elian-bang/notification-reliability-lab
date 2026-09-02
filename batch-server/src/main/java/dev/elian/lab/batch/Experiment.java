package dev.elian.lab.batch;

import dev.elian.lab.batch.jobs.ChunkSendJob;
import dev.elian.lab.batch.jobs.TaskletSendJob;
import dev.elian.lab.common.JobShape;
import dev.elian.lab.common.Knobs;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * chunk 크기와 트랜잭션 경계를 성능 기준으로 결정하기 위한 실험.
 *
 * <p>chunk 크기는 노브 하나로 셋을 동시에 움직인다. 키우면 커밋 횟수가 줄어 처리량은
 * 오르지만, 트랜잭션이 길어져 락을 오래 쥐고 메모리도 는다. 그래서 최적점이 있다.
 * 값 하나를 고르는 게 아니라 곡선을 그려 균형점을 찾는다.
 */
@Component
public class Experiment implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final Seeder seeder;
    private final RunControl control;
    private final TaskletSendJob taskletJob;
    private final ChunkSendJob chunkJob;
    private final JobLauncher launcher;

    @Value("${lab.tenants}")             int tenants;
    @Value("${lab.accounts-per-tenant}") int accountsPerTenant;
    @Value("${lab.requests}")              int requests;
    @Value("${lab.publish-latency-micros:0}") long publishLatency;
    @Value("${lab.warmup-millis}")         long warmup;
    @Value("${lab.repeat:1}")              int repeat;
    @Value("${lab.chunk-sizes:50,100,200,500,1000,2000}") String chunkSizes;

    public Experiment(JdbcTemplate jdbc, Seeder seeder, RunControl control,
                      TaskletSendJob taskletJob, ChunkSendJob chunkJob, JobLauncher launcher) {
        this.jdbc = jdbc; this.seeder = seeder; this.control = control;
        this.taskletJob = taskletJob; this.chunkJob = chunkJob; this.launcher = launcher;
    }

    /** 축 1 — chunk 크기 스윕. 경계는 운영 최초 조건(조회·큐발행 모두 TX 안)으로 고정. */
    private List<Knobs> sweepChunk() {
        List<Knobs> out = new ArrayList<>();
        Knobs base = Knobs.v0().shape(JobShape.CHUNK);
        for (String s : chunkSizes.split(",")) {
            int n = Integer.parseInt(s.trim());
            out.add(base.chunkSize(n).id("C" + n, "chunk " + n));
        }
        out.add(Knobs.v0().shape(JobShape.TASKLET).chunkSize(requests).id("TASKLET", "Tasklet 전건 1트랜잭션"));
        out.add(Knobs.v0().shape(JobShape.PER_REQUEST).chunkSize(500).id("PER-REQ", "건별 REQUIRES_NEW (운영 최초)"));
        return out;
    }

    /** 축 2 — 같은 chunk 크기에서 트랜잭션 경계만 바꾼다. */
    private List<Knobs> sweepBoundary(int chunk) {
        Knobs base = Knobs.v0().shape(JobShape.CHUNK).chunkSize(chunk);
        return List.of(
                base.id("B-in-in", "chunk " + chunk + " / 조회 TX내 · 큐발행 TX내"),
                base.queryOutsideTx().id("B-out-in", "chunk " + chunk + " / 조회 TX밖 · 큐발행 TX내"),
                base.publishOutsideTx().id("B-in-out", "chunk " + chunk + " / 조회 TX내 · 큐발행 TX밖"),
                base.queryOutsideTx().publishOutsideTx().id("B-out-out", "chunk " + chunk + " / 둘 다 TX밖")
        );
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.printf("=== chunk 크기 · 트랜잭션 경계 결정 실험 ===%n계정 %d · 발송요청 %d · 큐발행지연 %dus · 반복 %d회%n%n",
                tenants * accountsPerTenant, requests, publishLatency, repeat);

        System.out.println("[축 1] chunk 크기 스윕");
        Map<Knobs, List<Result>> chunkAxis = runAll(sweepChunk());

        int knee = pickKnee(chunkAxis);
        System.out.printf("%n[축 2] 트랜잭션 경계 (chunk %d 기준)%n", knee);
        Map<Knobs, List<Result>> boundaryAxis = runAll(sweepBoundary(knee));

        report(chunkAxis, boundaryAxis, knee);
    }

    /**
     * 균형점 = 처리량이 포화된 뒤 가장 <b>작은</b> chunk.
     *
     * <p>최대 처리량 지점을 고르면 안 된다. chunk 를 키우면 처리량은 몇 퍼센트 더 오르지만
     * 트랜잭션 길이는 배로 늘어난다. 그 구간은 손해다.
     * 최대 대비 3% 이내에 처음 들어오는 지점을 균형점으로 본다.
     */
    private int pickKnee(Map<Knobs, List<Result>> axis) {
        double maxTps = axis.entrySet().stream()
                .filter(e -> !e.getKey().id().equals("TASKLET"))
                .mapToDouble(e -> med(e.getValue(), Result::tps)).max().orElse(0);
        int best = 500;
        for (Map.Entry<Knobs, List<Result>> e : axis.entrySet()) {
            if (e.getKey().id().equals("TASKLET")) continue;
            if (med(e.getValue(), Result::tps) >= maxTps * 0.97) { best = e.getKey().chunkSize(); break; }
        }
        return best;
    }

    private Map<Knobs, List<Result>> runAll(List<Knobs> list) throws Exception {
        Map<Knobs, List<Result>> out = new LinkedHashMap<>();
        for (Knobs k : list) {
            List<Result> runs = new ArrayList<>();
            for (int i = 1; i <= repeat; i++) runs.add(runOne(k, i));
            out.put(k, runs);
        }
        return out;
    }

    private Result runOne(Knobs k, int attempt) throws Exception {
        String runId = k.id() + "-" + System.currentTimeMillis();

        control.stop();
        seeder.setForeignKey(k.foreignKey());
        seeder.setRequestIndex(k.requestIndex());
        seeder.reset(k, tenants, accountsPerTenant, requests);
        control.start(runId, k);
        Thread.sleep(warmup);

        long dlBefore = Lab.innodbDeadlocks(jdbc);
        TxTimer timer = new TxTimer();
        Job job = k.shape() != JobShape.CHUNK
                ? taskletJob.build(k, publishLatency, k.chunkSize(), timer)
                : chunkJob.build(k, publishLatency, requests, timer);

        long elapsed, written, rollbacks, peakMb;
        try (HeapSampler heap = new HeapSampler()) {
            long t0 = System.currentTimeMillis();
            JobExecution exec = launcher.run(job,
                    new JobParametersBuilder().addString("runId", runId).toJobParameters());
            elapsed = System.currentTimeMillis() - t0;
            written = exec.getStepExecutions().stream().mapToLong(s -> s.getWriteCount()).sum();
            rollbacks = exec.getStepExecutions().stream().mapToLong(s -> s.getRollbackCount()).sum();
            peakMb = heap.peakMb();
        }

        Thread.sleep(1200);
        control.stop();
        Map<String, Object> recv = control.metric(runId, "receiver");
        long innodb = Lab.innodbDeadlocks(jdbc) - dlBefore;

        Result r = new Result(elapsed, written, rollbacks, peakMb,
                timer.count(), timer.meanMillis(), timer.percentileMillis(95), timer.maxMillis(),
                num(recv, "deadlocks", innodb));

        System.out.printf("  %-14s #%d | TPS %7.1f | TX %5d회 평균 %7.1fms p95 %7.1fms | 힙 %4dMB | 수신자 dl %3d%n",
                k.id(), attempt, r.tps(), r.txCount(), r.txMeanMs(), r.txP95Ms(), r.peakMb(), r.receiverDeadlocks());
        return r;
    }

    private static long num(Map<String, Object> m, String key, long fallback) {
        Object v = m.get(key);
        return v == null ? fallback : ((Number) v).longValue();
    }

    private record Result(long elapsedMillis, long written, long rollbacks, long peakMb,
                          int txCount, double txMeanMs, double txP95Ms, double txMaxMs,
                          long receiverDeadlocks) {
        double tps() { return elapsedMillis == 0 ? 0 : written * 1000.0 / elapsedMillis; }
    }

    private static double med(List<Result> runs, java.util.function.ToDoubleFunction<Result> f) {
        List<Double> v = runs.stream().map(f::applyAsDouble).sorted().toList();
        return v.isEmpty() ? 0 : v.get(v.size() / 2);
    }

    private void report(Map<Knobs, List<Result>> chunkAxis, Map<Knobs, List<Result>> boundaryAxis, int knee)
            throws Exception {
        StringBuilder sb = new StringBuilder("# chunk 크기 · 트랜잭션 경계 결정 실험\n\n");
        sb.append("chunk 크기 하나가 처리량 · 락 보유 시간 · 메모리를 동시에 움직인다.\n");
        sb.append("값을 고르는 게 아니라 곡선을 그려 균형점을 찾는다.\n\n");
        sb.append("고정값: 계정 %d · 발송요청 %d · 큐발행지연 %dus · 반복 %d회\n"
                .formatted(tenants * accountsPerTenant, requests, publishLatency, repeat));
        sb.append("MySQL 8.0 (2 CPU / 2GB), innodb_lock_wait_timeout=5s\n\n");
        sb.append("## 축 1 — chunk 크기 스윕\n\n트랜잭션 경계는 운영 최초 조건으로 고정(조회·큐발행 모두 TX 안).\n\n");
        sb.append(table(chunkAxis));
        sb.append("\n## 축 2 — 트랜잭션 경계 (chunk %d 기준)\n\n".formatted(knee));
        sb.append(table(boundaryAxis));
        sb.append("\n> 실험 환경의 결과다. 운영 환경의 수치가 아니다.\n");

        Path out = Path.of("results", "chunk_경계_결정.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString());
        System.out.println("\n→ " + out.toAbsolutePath());
    }

    private String table(Map<Knobs, List<Result>> axis) {
        StringBuilder sb = new StringBuilder();
        sb.append("| 구성 | 처리량 TPS | 트랜잭션 수 | TX 평균 | TX p95 | TX 최대 | 힙 peak | 수신자 데드락 | 롤백 |\n");
        sb.append("|---|---|---|---|---|---|---|---|---|\n");
        axis.forEach((k, runs) -> sb.append("| %s | **%.0f** | %d | %.1fms | %.1fms | %.1fms | %dMB | %d | %d |\n"
                .formatted(k.label(), med(runs, Result::tps), (int) med(runs, r -> r.txCount()),
                        med(runs, Result::txMeanMs), med(runs, Result::txP95Ms), med(runs, Result::txMaxMs),
                        (long) med(runs, r -> (double) r.peakMb()),
                        (long) med(runs, r -> (double) r.receiverDeadlocks()),
                        (long) med(runs, r -> (double) r.rollbacks()))));
        return sb.toString();
    }
}
