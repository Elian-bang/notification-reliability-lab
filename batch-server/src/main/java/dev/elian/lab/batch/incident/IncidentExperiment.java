package dev.elian.lab.batch.incident;

import dev.elian.lab.batch.Lab;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 사건 재현과 처방별 비교.
 *
 * <p>증상은 데드락(1213)이 아니라 <b>락 대기 타임아웃(1205)</b>이다.
 * 사이클이 없어도 나기 때문에 훨씬 쉽게 발생한다.
 */
@Component
@ConditionalOnProperty(name = "lab.mode", havingValue = "incident")
public class IncidentExperiment implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final ContentPushJob job;
    private final RunSignal signal;

    @Value("${lab.incident.contents:3}")          int contents;
    @Value("${lab.incident.millis-per-content:8000}") long millisPerContent;
    @Value("${lab.repeat:1}")                     int repeat;

    public IncidentExperiment(JdbcTemplate jdbc, ContentPushJob job, RunSignal signal) {
        this.jdbc = jdbc; this.job = job; this.signal = signal;
    }

    private List<IncidentKnobs> variants() {
        IncidentKnobs asIs = IncidentKnobs.asHappened();
        return List.of(
                asIs,
                asIs.withCommitPerContent().id("FIX-1", "① 완료표시 건별 커밋"),
                asIs.viewSeparateTx().id("FIX-2", "② 조회수를 본문 조회에서 분리"),
                asIs.commitThenPush().id("FIX-3", "③ 커밋 후 푸시"),
                asIs.lockWait(5).id("FIX-4", "④ lock_wait_timeout 50s → 5s"),
                asIs.withCommitPerContent().viewSeparateTx().id("FIX-1+2", "①+②")
        );
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.printf("=== 공지 푸시 사건 재현 ===%n콘텐츠 %d건 · 건당 발송 %dms · 반복 %d회%n%n",
                contents, millisPerContent, repeat);

        Map<IncidentKnobs, List<Result>> results = new LinkedHashMap<>();
        for (IncidentKnobs k : variants()) {
            List<Result> runs = new ArrayList<>();
            for (int i = 1; i <= repeat; i++) runs.add(runOne(k, i));
            results.put(k, runs);
        }
        report(results);
    }

    private Result runOne(IncidentKnobs k, int attempt) throws Exception {
        jdbc.update("UPDATE experiment_control SET active = 0, hot_content_seq = NULL WHERE id = 1");
        seed();
        jdbc.update("UPDATE experiment_control SET mode = 'CONTENT', view_in_same_tx = ?, run_id = ?, variant_id = ?, active = 1, updated_at = NOW(6) WHERE id = 1",
                k.viewInSameTx() ? 1 : 0, k.id() + "-" + System.currentTimeMillis(), k.id());
        jdbc.execute("SET GLOBAL innodb_lock_wait_timeout = " + k.lockWaitTimeoutSec());
        Thread.sleep(1500);

        String runId = jdbc.queryForObject("SELECT run_id FROM experiment_control WHERE id = 1", String.class);
        long dlBefore = Lab.innodbDeadlocks(jdbc);

        List<ContentPushJob.Content> list = new ArrayList<>();
        for (int i = 1; i <= contents; i++) list.add(new ContentPushJob.Content(8400 + i, 8540));

        long t0 = System.currentTimeMillis();
        job.run(list, k, millisPerContent, signal);
        long elapsed = System.currentTimeMillis() - t0;

        Thread.sleep(2000);
        signal.clear();
        jdbc.update("UPDATE experiment_control SET active = 0 WHERE id = 1");

        Map<String, Object> m = metric(runId);
        Result r = new Result(elapsed,
                n(m, "attempted"), n(m, "committed"), n(m, "lock_timeouts"), n(m, "deadlocks"),
                n(m, "p95_millis"), Lab.innodbDeadlocks(jdbc) - dlBefore);

        System.out.printf("  %-8s #%d %-34s | 조회 %4d건 중 실패 %3d (타임아웃 %3d) | p95 %6dms | 배치 %.1fs%n",
                k.id(), attempt, k.describe(), r.attempted(), r.failed(), r.lockTimeouts(), r.p95Millis(), elapsed / 1000.0);
        return r;
    }

    private void seed() {
        jdbc.execute("TRUNCATE TABLE content_item");
        for (int i = 1; i <= contents; i++) {
            jdbc.update("INSERT INTO content_item (seq, title, view_count, status) VALUES (?, ?, 0, 'READY')",
                    8400 + i, "콘텐츠 " + (8400 + i));
        }
    }

    private Map<String, Object> metric(String runId) {
        try { return jdbc.queryForMap("SELECT * FROM experiment_metric WHERE run_id = ? AND role = 'viewer'", runId); }
        catch (RuntimeException e) { return Map.of(); }
    }

    private static long n(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? 0 : ((Number) v).longValue();
    }

    private record Result(long elapsedMillis, long attempted, long committed,
                          long lockTimeouts, long deadlocks, long p95Millis, long innodbDeadlocks) {
        long failed() { return lockTimeouts + deadlocks; }
        double failRatePct() { return attempted == 0 ? 0 : failed() * 100.0 / attempted; }
    }

    private static double med(List<Result> runs, java.util.function.ToDoubleFunction<Result> f) {
        List<Double> v = runs.stream().map(f::applyAsDouble).sorted().toList();
        return v.isEmpty() ? 0 : v.get(v.size() / 2);
    }

    private void report(Map<IncidentKnobs, List<Result>> results) throws Exception {
        StringBuilder sb = new StringBuilder("# 공지 푸시 사건 재현과 처방 비교\n\n");
        sb.append("배치가 콘텐츠 %d건을 한 트랜잭션으로 처리한다. 건당 발송 %dms.\n".formatted(contents, millisPerContent));
        sb.append("첫 콘텐츠는 자기 발송이 끝나면 완료 표시를 하지만, 커밋은 마지막 콘텐츠까지 끝나야 일어난다.\n");
        sb.append("그동안 그 행은 잠겨 있고, 푸시를 받고 들어온 사용자의 조회수 UPDATE가 대기한다.\n\n");
        sb.append("**UPDATE 자체는 수 ms다. 오래 잠근 게 아니라 안 풀어준 것이다.**\n\n");
        sb.append("| 구성 | 설정 | 조회 시도 | **실패** | 실패율 | 락 타임아웃 | 조회 p95 | 배치 소요 |\n");
        sb.append("|---|---|---|---|---|---|---|---|\n");
        results.forEach((k, runs) -> sb.append("| %s | <sub>%s</sub> | %d | **%d** | %.1f%% | %d | %dms | %.1fs |\n"
                .formatted(k.label(), k.describe(),
                        (long) med(runs, r -> (double) r.attempted()),
                        (long) med(runs, r -> (double) r.failed()),
                        med(runs, Result::failRatePct),
                        (long) med(runs, r -> (double) r.lockTimeouts()),
                        (long) med(runs, r -> (double) r.p95Millis()),
                        med(runs, r -> r.elapsedMillis() / 1000.0))));
        sb.append("\n> 실험 환경의 결과다. 운영 환경의 수치가 아니다.\n");

        Path out = Path.of("results", "사건재현_처방비교.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString());
        System.out.println("\n→ " + out.toAbsolutePath());
    }
}
