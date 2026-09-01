package dev.elian.lab.batch;

import dev.elian.lab.common.Variant;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * chunk 트랜잭션 안에서 실행된다.
 *
 * <p><b>member_account 를 갱신하는 문장이 한 줄도 없다.</b>
 * 그런데 notification INSERT 시 FK 검사가 부모 행에 S락을 건다 — 그게 데드락의 연결 고리다.
 */
public class SendItemWriter implements ItemWriter<SendTarget> {

    static final String INSERT_NOTI =
            "INSERT INTO notification (account_id, request_id, status, created_at) "
          + "VALUES (?, ?, 'CREATED', NOW(6))";
    static final String UPDATE_NOTI_SENT =
            "UPDATE notification SET status = 'SENT', sent_at = NOW(6) WHERE request_id = ?";
    static final String UPDATE_REQ_DONE =
            "UPDATE notification_request SET status = 'DONE', finished_at = NOW(6) WHERE id = ?";

    private final JdbcTemplate jdbc;
    private final Variant variant;
    private final long apiDelayMicros;

    public SendItemWriter(JdbcTemplate jdbc, Variant variant, long apiDelayMicros) {
        this.jdbc = jdbc;
        this.variant = variant;
        this.apiDelayMicros = apiDelayMicros;
    }

    @Override
    public void write(Chunk<? extends SendTarget> chunk) {
        List<? extends SendTarget> items = chunk.getItems();
        if (items.isEmpty()) return;

        if (variant.batchUpdate()) {
            jdbc.batchUpdate(INSERT_NOTI,
                    items.stream().map(t -> new Object[]{t.accountId(), t.requestId()}).toList());
            spin(apiDelayMicros);
            jdbc.batchUpdate(UPDATE_NOTI_SENT,
                    items.stream().map(t -> new Object[]{t.requestId()}).toList());
            jdbc.batchUpdate(UPDATE_REQ_DONE,
                    items.stream().map(t -> new Object[]{t.requestId()}).toList());
            return;
        }

        for (SendTarget t : items) {
            jdbc.update(INSERT_NOTI, t.accountId(), t.requestId());  // 자원 A + FK로 계정에 S락
            spin(apiDelayMicros);                                     // 외부 채널사 API 대기
            jdbc.update(UPDATE_NOTI_SENT, t.requestId());
            jdbc.update(UPDATE_REQ_DONE, t.requestId());
        }
    }

    /** Thread.sleep 은 마이크로초 해상도가 없어서 스핀으로 대기한다. */
    private static void spin(long micros) {
        if (micros <= 0) return;
        long until = System.nanoTime() + micros * 1000L;
        while (System.nanoTime() < until) Thread.onSpinWait();
    }
}
