package dev.elian.lab.batch.incident;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 어느 콘텐츠에 푸시가 나갔는지 수신자 쪽에 알린다.
 *
 * <p>사건의 핵심 — 푸시를 받은 사람이 곧 그 콘텐츠를 열 사람이다.
 * <b>배치가 스스로, 자기가 잠근 행에 부딪힐 트래픽을 만들어낸다.</b>
 * 유입을 무작위로 두면 이 구조가 재현되지 않는다.
 */
@Component
public class RunSignal {

    private final JdbcTemplate jdbc;

    public RunSignal(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** 이 콘텐츠로 사용자를 유도한다. */
    public void pushSent(long contentSeq) {
        jdbc.update("UPDATE experiment_control SET hot_content_seq = ?, updated_at = NOW(6) WHERE id = 1", contentSeq);
    }

    public void clear() {
        jdbc.update("UPDATE experiment_control SET hot_content_seq = NULL WHERE id = 1");
    }
}
