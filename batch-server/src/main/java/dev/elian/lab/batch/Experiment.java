package dev.elian.lab.batch;

import dev.elian.lab.batch.jobs.ChunkSendJob;
import dev.elian.lab.batch.jobs.TaskletSendJob;
import dev.elian.lab.common.JobShape;
import dev.elian.lab.common.Knobs;
import dev.elian.lab.common.SendMode;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 변수 목록은 <b>운영에서 실제로 밟은 순서</b> 그대로다.
 *
 * <pre>
 *   ① Tasklet                     → 데드락 발생        (V0)
 *   ② chunk 전환                  → 여기서도 발생      (V1)  ★ 핵심 관찰
 *   ③ 트랜잭션 단축 (조회를 밖으로)                     (V2)
 *   ④ 발송 방식 변경 — 셋 다 시도                       (V3a/V3b/V3c)
 *        a. 건별 → 벌크로 묶기
 *        b. 알림 행 미리 생성, 배치는 상태만 UPDATE
 *        c. 외부 API 호출을 트랜잭션 밖으로
 *   → 0건
 * </pre>
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
    @Value("${lab.api-delay-micros}")      long apiDelay;
    @Value("${lab.warmup-millis}")         long warmup;
    @Value("${lab.only:}")                 String only;

    public Experiment(JdbcTemplate jdbc, Seeder seeder, RunControl control,
                      TaskletSendJob taskletJob, ChunkSendJob chunkJob, JobLauncher launcher) {
        this.jdbc = jdbc; this.seeder = seeder; this.control = control;
        this.taskletJob = taskletJob; this.chunkJob = chunkJob; this.launcher = launcher;
    }

    private List<Knobs> variants() {
        Knobs v0 = Knobs.v0();                                   // ① Tasklet
        Knobs v1 = v0.shape(JobShape.CHUNK).id("V1", "② chunk 전환");
        return List.of(
                v0,
                v1,
                v1.queryOutsideTx().id("V2", "③ 조회를 TX 밖으로"),
                v1.sendMode(SendMode.BULK).id("V3a", "④a 벌크로 묶기"),
                v1.sendMode(SendMode.UPDATE_ONLY).id("V3b", "④b 알림 선생성·상태만 UPDATE"),
                v1.apiCallOutsideTx().id("V3c", "④c 외부 API를 TX 밖으로"),
                v1.queryOutsideTx().sendMode(SendMode.BULK).apiCallOutsideTx()
                  .id("V6", "운영 최종 조합 (③+④a+④c)"),
                v1.withReceiverUnify().id("V4", "수신자 순서 통일"),
                v1.isolation(Knobs.RC).id("V5", "READ COMMITTED"),
                v1.noForeignKey().id("V8", "FK 제거"),
                v1.withReceiverSplitTx().id("V9", "수신자 TX 분리")
        );
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.printf("""
                === A-M8 Deadlock Matrix ===
                테넌트 %d · 계정 %d · 발송요청 %d · 외부API지연 %dus
                %n""", tenants, tenants * accountsPerTenant, requests, apiDelay);

        Map<Knobs, Result> results = new LinkedHashMap<>();
        for (Knobs k : variants()) {
            if (only != null && !only.isBlank() && !List.of(only.split(",")).contains(k.id())) continue;
            results.put(k, runOne(k));
        }
        report(results);
    }

    private Result runOne(Knobs k) throws Exception {
        String runId = k.id() + "-" + System.currentTimeMillis();

        control.stop();
        seeder.setForeignKey(k.foreignKey());
        seeder.reset(k, tenants, accountsPerTenant, requests);
        control.start(runId, k);
        Thread.sleep(warmup);                     // 수신자 부하가 붙을 시간

        long dlBefore = Lab.innodbDeadlocks(jdbc);
        Job job = k.shape() == JobShape.TASKLET
                ? taskletJob.build(k, apiDelay, k.chunkSize())
                : chunkJob.build(k, apiDelay, requests);

        long t0 = System.currentTimeMillis();
        JobExecution exec = launcher.run(job,
                new JobParametersBuilder().addString("runId", runId).toJobParameters());
        long elapsed = System.currentTimeMillis() - t0;

        long rollbacks = exec.getStepExecutions().stream().mapToLong(s -> s.getRollbackCount()).sum();
        long written = exec.getStepExecutions().stream().mapToLong(s -> s.getWriteCount()).sum();

        String dump = Lab.latestDeadlock(jdbc);
        Thread.sleep(1200);                       // 수신자 지표 flush 대기
        control.stop();

        Map<String, Object> recv = control.metric(runId, "receiver");
        long innodb = Lab.innodbDeadlocks(jdbc) - dlBefore;

        Result r = new Result(rollbacks, written, exec.getExitStatus().getExitCode(),
                recv, elapsed, innodb, dump);
        System.out.printf("%-4s %-30s | 배치 롤백=%3d 기록=%5d %-9s | 수신자 %sdl/%sto | InnoDB=%2d | %.1fs%n",
                k.id(), k.label(), rollbacks, written, r.status(),
                g(recv, "deadlocks"), g(recv, "lock_timeouts"), innodb, elapsed / 1000.0);
        return r;
    }

    private record Result(long rollbacks, long written, String status, Map<String, Object> receiver,
                          long elapsedMillis, long innodbDeadlocks, String dump) {}

    private static Object g(Map<String, Object> m, String k) { return m.isEmpty() ? "__" : m.get(k); }

    private void report(Map<Knobs, Result> results) throws Exception {
        StringBuilder sb = new StringBuilder("# A-M8 Deadlock Matrix — 측정 결과\n\n");
        sb.append("""
                배치(알림 A → FK로 계정 B) ↔ 수신자(계정 B → 알림 A)
                **배치는 member_account 를 갱신하지 않는다. 계정 락은 FK 로만 걸린다.**

                고정값: 테넌트 %d · 계정 %d · 발송요청 %d · 외부API지연 %dus
                MySQL 8.0 (2 CPU / 2GB), innodb_lock_wait_timeout=5s

                """.formatted(tenants, tenants * accountsPerTenant, requests, apiDelay));
        sb.append("| # | 구성 | 배치 롤백 | 기록 | **수신자 데드락** | 수신자 타임아웃 | 토큰갱신 실패 | 알림확인 실패 | InnoDB | 소요 |\n");
        sb.append("|---|---|---|---|---|---|---|---|---|---|\n");
        results.forEach((k, r) -> sb.append("| %s | %s<br><sub>%s</sub> | %d | %d | **%s** | %s | %s | %s | %d | %.1fs |\n"
                .formatted(k.id(), k.label(), k.describe(), r.rollbacks(), r.written(),
                        g(r.receiver(), "deadlocks"), g(r.receiver(), "lock_timeouts"),
                        g(r.receiver(), "step_token_fail"), g(r.receiver(), "step_read_fail"),
                        r.innodbDeadlocks(), r.elapsedMillis() / 1000.0)));
        sb.append("\n> 실험 환경의 결과다. 운영 환경의 수치가 아니다.\n");

        results.forEach((k, r) -> {
            if (!r.dump().isBlank()) {
                sb.append("\n---\n\n## ").append(k.id()).append(" — LATEST DETECTED DEADLOCK\n\n```\n")
                  .append(r.dump()).append("\n```\n");
            }
        });

        Path out = Path.of("results", "A-M8_결과.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString());
        System.out.println("\n→ " + out.toAbsolutePath());
    }
}
