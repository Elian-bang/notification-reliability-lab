package dev.elian.lab.batch;

import dev.elian.lab.common.Variant;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExperimentRunner implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final Coordinator coord;
    private final JobFactory jobFactory;
    private final JobLauncher jobLauncher;

    @Value("${lab.tenants}")             int tenants;
    @Value("${lab.accounts-per-tenant}") int accountsPerTenant;
    @Value("${lab.requests}")              int requests;
    @Value("${lab.api-delay-micros}")      long apiDelay;
    @Value("${lab.warmup-millis}")         long warmup;
    @Value("${lab.only:}")                 String only;

    public ExperimentRunner(JdbcTemplate jdbc, Coordinator coord, JobFactory jobFactory, JobLauncher jobLauncher) {
        this.jdbc = jdbc; this.coord = coord; this.jobFactory = jobFactory; this.jobLauncher = jobLauncher;
    }

    private List<Variant> variants() {
        Variant v0 = Variant.v0();
        return List.of(
                v0,
                v0.queryOutside().name("V1", "조회를 TX 밖으로"),
                v0.chunk(200).name("V2", "chunk 500→200 (탐색 과정)"),
                v0.batch().name("V3", "BATCH UPDATE"),
                v0.unifyOrder().name("V4", "수신자 순서 통일"),
                v0.isolation(Variant.RC).name("V5", "READ COMMITTED"),
                v0.queryOutside().batch().name("V6", "V1+V3 (운영 조합)"),
                v0.noForeignKey().name("V8", "FK 제거"),
                v0.splitTx().name("V9", "수신자 TX 분리")
        );
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.printf("""
                === A-M8 Deadlock Matrix ===
                테넌트 %d · 계정 %d · 발송요청 %d · API지연 %dus
                배치: Spring Batch chunk-oriented (chunk = 트랜잭션)
                %n""", tenants, tenants * accountsPerTenant, requests, apiDelay);

        Map<Variant, Result> results = new LinkedHashMap<>();
        for (Variant v : variants()) {
            if (only != null && !only.isBlank() && !List.of(only.split(",")).contains(v.id())) continue;
            results.put(v, runVariant(v));
        }
        report(results);
    }

    private Result runVariant(Variant v) throws Exception {
        String runId = v.id() + "-" + System.currentTimeMillis();

        coord.deactivate();
        coord.setForeignKey(v.foreignKey());
        coord.resetData(tenants, accountsPerTenant, requests);
        coord.activate(runId, v);
        Thread.sleep(warmup);                       // Node 쪽 부하가 붙을 시간

        long dlBefore = Lab.innodbDeadlocks(jdbc);
        Job job = jobFactory.build(v, apiDelay, requests);

        long t0 = System.currentTimeMillis();
        var exec = jobLauncher.run(job, new JobParametersBuilder()
                .addString("runId", runId)
                .toJobParameters());
        long elapsed = System.currentTimeMillis() - t0;

        // Spring Batch 는 chunk 트랜잭션 실패를 StepExecution 에 모아둔다
        long rollbacks = exec.getStepExecutions().stream()
                .mapToLong(se -> se.getRollbackCount()).sum();
        long written = exec.getStepExecutions().stream()
                .mapToLong(se -> se.getWriteCount()).sum();
        String status = exec.getExitStatus().getExitCode();

        String dump = Lab.latestDeadlock(jdbc);
        Thread.sleep(1200);                          // Node 쪽 지표 flush 대기
        coord.deactivate();

        Map<String, Object> recv = coord.readMetric(runId, "receiver");
        Map<String, Object> req = coord.readMetric(runId, "request");
        long innodbDelta = Lab.innodbDeadlocks(jdbc) - dlBefore;

        Result r = new Result(rollbacks, written, status, recv, req, elapsed, innodbDelta, dump);
        System.out.printf("%-3s %-24s | 배치 롤백=%3d 기록=%5d %s | 수신자 %s | InnoDB dl=%d | %.1fs%n",
                v.id(), v.label(), rollbacks, written, status,
                recv.isEmpty() ? "미수집" : recv.get("deadlocks") + "dl/" + recv.get("lock_timeouts") + "to",
                innodbDelta, elapsed / 1000.0);
        return r;
    }

    private record Result(long batchRollbacks, long written, String status,
                          Map<String, Object> receiver, Map<String, Object> request,
                          long elapsedMillis, long innodbDelta, String dump) {}

    private static Object g(Map<String, Object> m, String k) { return m.isEmpty() ? "__" : m.get(k); }

    private void report(Map<Variant, Result> results) throws Exception {
        StringBuilder sb = new StringBuilder("# A-M8 Deadlock Matrix — 측정 결과\n\n");
        sb.append("""
                토폴로지: 배치(알림 A → FK로 계정 B) ↔ 수신자(계정 B → 알림 A)
                **배치는 member_account 를 갱신하지 않는다. 계정 락은 FK 로만 걸린다.**

                고정값: 테넌트 %d · 계정 %d · 발송요청 %d · API지연 %dus
                MySQL 8.0 (2 CPU / 2GB), innodb_lock_wait_timeout=5s
                배치: Spring Batch chunk-oriented step (chunk = 트랜잭션)

                """.formatted(tenants, tenants * accountsPerTenant, requests, apiDelay));
        sb.append("| # | 구성 | 배치 롤백 | 기록 건수 | 수신자 데드락 | 수신자 타임아웃 | 토큰갱신 실패 | 알림확인 실패 | InnoDB dl | 소요 |\n");
        sb.append("|---|---|---|---|---|---|---|---|---|---|\n");
        results.forEach((v, r) -> sb.append("| %s | %s<br><sub>%s</sub> | %d | %d | %s | %s | %s | %s | %d | %.1fs |\n"
                .formatted(v.id(), v.label(), v.describe(),
                        r.batchRollbacks(), r.written(),
                        g(r.receiver(), "deadlocks"), g(r.receiver(), "lock_timeouts"),
                        g(r.receiver(), "step_token_fail"), g(r.receiver(), "step_read_fail"),
                        r.innodbDelta(), r.elapsedMillis() / 1000.0)));
        sb.append("\n> 실험 환경의 결과다. 운영 환경의 수치가 아니다.\n");

        results.forEach((v, r) -> {
            if (!r.dump().isBlank()) {
                sb.append("\n---\n\n## ").append(v.id()).append(" — LATEST DETECTED DEADLOCK\n\n```\n")
                  .append(r.dump()).append("\n```\n");
            }
        });

        Path out = Path.of("results", "A-M8_결과.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString());
        System.out.println("\n→ " + out.toAbsolutePath());
    }
}
