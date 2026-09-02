package dev.elian.lab.batch.incident;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 공지 푸시 배치 재현.
 *
 * <p>콘텐츠 여러 건을 <b>한 트랜잭션</b>으로 돈다. 각 콘텐츠는
 * ① 발송(무겁다. 별도 트랜잭션이라 락을 안 남긴다) → ② 완료 표시(가볍다. <b>바깥 트랜잭션에서 행을 잠근다</b>)
 * 순서다. 커밋은 마지막 콘텐츠까지 끝나야 일어난다.
 */
@Component
public class ContentPushJob {

    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager txManager;

    public ContentPushJob(JdbcTemplate jdbc, PlatformTransactionManager txManager) {
        this.jdbc = jdbc;
        this.txManager = txManager;
    }

    private TransactionTemplate tx(int propagation) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(propagation);
        return new TransactionTemplate(txManager, def);
    }

    public record Content(long seq, int audience) {}

    /** 콘텐츠 한 건 발송에 걸리는 시간을 흉내낸다 (사건에서는 8,540명에 72초). */
    private void sendPush(Content c, long millisPerContent) {
        // 발송은 알림 테이블·FCM 경로다. 콘텐츠 행을 건드리지 않으므로 락을 남기지 않는다.
        // 사건에서도 푸시 8,540건은 정상 발송됐다 — 깨진 건 상세 페이지 쪽이었다.
        sleep(millisPerContent);
    }

    private void markDone(Content c, IncidentKnobs k) {
        String sql = "UPDATE content_item SET status = 'SENT', sent_at = NOW(6) WHERE seq = ?";
        if (k.commitPerContent()) {
            // 처방 ① — 완료 표시를 콘텐츠마다 즉시 커밋한다. 락이 바로 풀린다
            tx(TransactionDefinition.PROPAGATION_REQUIRES_NEW).executeWithoutResult(s -> jdbc.update(sql, c.seq()));
        } else {
            // 사건 당시 — 바깥 트랜잭션에서 UPDATE. 커밋까지 행이 잠긴 채로 남는다
            jdbc.update(sql, c.seq());
        }
    }

    /** 배치 한 번. 콘텐츠 전체가 한 트랜잭션이다. */
    public void run(List<Content> contents, IncidentKnobs k, long millisPerContent, RunSignal signal) {
        // 푸시를 받은 사람은 20~90초에 걸쳐 들어온다. 첫 콘텐츠가 가장 오래 잠기고(사건의 A),
        // 그 창에 도달한 클릭이 부딪힌다. 유입을 그 콘텐츠로 고정한다.
        signal.pushSent(contents.get(0).seq());
        tx(TransactionDefinition.PROPAGATION_REQUIRES_NEW).executeWithoutResult(s -> {
            for (Content c : contents) {
                if (k.commitBeforePush()) {
                    // 처방 ③ — 완료 표시를 먼저 커밋하고 푸시를 보낸다.
                    // 푸시를 받은 사용자가 들어올 때 그 행은 이미 풀려 있다
                    markDone(c, k);
                    sendPush(c, millisPerContent);
                } else {
                    sendPush(c, millisPerContent);
                    markDone(c, k);
                }
            }
        });
    }

    private static void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
