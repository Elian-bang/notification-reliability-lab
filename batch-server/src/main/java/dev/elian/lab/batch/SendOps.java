package dev.elian.lab.batch;

import dev.elian.lab.common.Knobs;
import dev.elian.lab.common.SendMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 발송 처리 SQL. Tasklet 과 chunk 가 <b>같은 코드를 쓴다</b> —
 * 그래야 두 형태의 차이가 "트랜잭션 경계" 하나로만 갈린다.
 *
 * <p><b>member_account 를 갱신하는 문장은 한 줄도 없다.</b>
 * 계정 행 락은 notification INSERT 시 FK 검사로만 걸린다.
 */
@Component
public class SendOps {

    static final String SELECT_PENDING =
            "SELECT id, account_id FROM notification_request WHERE status = 'PENDING' ORDER BY id LIMIT ?";
    static final String INSERT_NOTI =
            "INSERT INTO notification (account_id, request_id, status, created_at) VALUES (?, ?, 'CREATED', NOW(6))";
    static final String UPDATE_NOTI_SENT_BY_REQ =
            "UPDATE notification SET status = 'SENT', sent_at = NOW(6) WHERE request_id = ?";
    static final String UPDATE_REQ_DONE =
            "UPDATE notification_request SET status = 'DONE', finished_at = NOW(6) WHERE id = ?";

    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager txManager;

    public SendOps(JdbcTemplate jdbc, PlatformTransactionManager txManager) {
        this.jdbc = jdbc;
        this.txManager = txManager;
    }

    /**
     * 건별로 트랜잭션을 따로 연다 (REQUIRES_NEW). 운영 최초 구조.
     *
     * <p>트랜잭션은 아주 짧다. 그런데도 데드락이 난다 —
     * <b>FK 검사가 부모 행에 거는 S락은 트랜잭션이 짧아져도 사라지지 않기 때문이다.</b>
     * 짧아진 것은 락을 쥐는 <i>시간</i>이지 락을 쥐는 <i>사실</i>이 아니다.
     */
    public void sendPerRequestTx(java.util.List<SendTarget> targets, Knobs knobs, long publishLatencyMicros) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        def.setIsolationLevel(Knobs.RC.equals(knobs.isolation())
                ? TransactionDefinition.ISOLATION_READ_COMMITTED
                : TransactionDefinition.ISOLATION_REPEATABLE_READ);
        TransactionTemplate tx = new TransactionTemplate(txManager, def);
        for (SendTarget t : targets) {
            tx.executeWithoutResult(s -> send(java.util.List.of(t), knobs, publishLatencyMicros));
        }
    }

    public List<SendTarget> findPending(int limit) {
        return jdbc.query(SELECT_PENDING,
                (rs, i) -> new SendTarget(rs.getLong("id"), rs.getLong("account_id")), limit);
    }

    /**
     * 대상 묶음 하나를 발송 처리한다.
     *
     * @param publishLatencyMicros 큐 발행 비용. <b>발송은 큐로 비동기 요청하므로 트랜잭션 안에서
     *                       채널사 응답을 기다리지 않는다.</b> 기본값 0 이 실제 동작이고,
     *                       값을 주면 "동기 호출이었다면" 대조군이 된다.
     */
    public void send(List<SendTarget> targets, Knobs knobs, long publishLatencyMicros) {
        if (targets.isEmpty()) return;

        if (knobs.sendMode() == SendMode.UPDATE_ONLY) {
            // 알림 행이 미리 만들어져 있다 → INSERT 가 없으므로 FK 부모 행 S락도 없다
            for (SendTarget t : targets) {
                jdbc.update(UPDATE_NOTI_SENT_BY_REQ, t.requestId());
                spin(publishLatencyMicros);
                jdbc.update(UPDATE_REQ_DONE, t.requestId());
            }
            return;
        }

        if (knobs.sendMode() == SendMode.BULK) {
            List<Object[]> ins = targets.stream().map(t -> new Object[]{t.accountId(), t.requestId()}).toList();
            List<Object[]> ids = targets.stream().map(t -> new Object[]{t.requestId()}).toList();
            jdbc.batchUpdate(INSERT_NOTI, ins);      // FK S락 (묶어서 한 번)
            spin(publishLatencyMicros);
            jdbc.batchUpdate(UPDATE_NOTI_SENT_BY_REQ, ids);
            jdbc.batchUpdate(UPDATE_REQ_DONE, ids);
            return;
        }

        // PER_ROW — 최초 상태
        for (SendTarget t : targets) {
            jdbc.update(INSERT_NOTI, t.accountId(), t.requestId());  // 자원 A + FK로 계정에 S락
            spin(publishLatencyMicros);                              // 큐 발행 (기본 0 — 대기하지 않는다)
            jdbc.update(UPDATE_NOTI_SENT_BY_REQ, t.requestId());
            jdbc.update(UPDATE_REQ_DONE, t.requestId());
        }
    }

    /** 큐 발행을 트랜잭션 밖으로 뺀 경우 (dual-write 대신 커밋 후 발행). */
    public static void publishOutside(long micros) { spin(micros); }

    private static void spin(long micros) {
        if (micros <= 0) return;
        long until = System.nanoTime() + micros * 1000L;
        while (System.nanoTime() < until) Thread.onSpinWait();
    }
}
