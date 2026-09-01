package dev.elian.lab.batch;

import dev.elian.lab.common.Knobs;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 서버 3개를 DB 로 동기화한다. 배치가 신호를 쓰고, Node 쪽(수신자·요청)이 폴링한다.
 * HTTP 조율 대신 DB 를 쓴 이유: 의존성이 없고 어차피 셋 다 같은 DB 를 본다.
 */
@Component
public class RunControl {

    private final JdbcTemplate jdbc;

    public RunControl(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void start(String runId, Knobs k) {
        jdbc.update("""
                INSERT INTO experiment_control
                    (id, run_id, variant_id, receiver_unify, receiver_tx_split, isolation_level, active, updated_at)
                VALUES (1, ?, ?, ?, ?, ?, 1, NOW(6))
                ON DUPLICATE KEY UPDATE
                    run_id=VALUES(run_id), variant_id=VALUES(variant_id),
                    receiver_unify=VALUES(receiver_unify), receiver_tx_split=VALUES(receiver_tx_split),
                    isolation_level=VALUES(isolation_level), active=1, updated_at=NOW(6)
                """,
                runId, k.id(), k.receiverUnify() ? 1 : 0, k.receiverTxSplit() ? 1 : 0, k.isolation());
    }

    public void stop() {
        jdbc.update("UPDATE experiment_control SET active = 0, updated_at = NOW(6) WHERE id = 1");
    }

    public Map<String, Object> metric(String runId, String role) {
        try {
            return jdbc.queryForMap("SELECT * FROM experiment_metric WHERE run_id = ? AND role = ?", runId, role);
        } catch (RuntimeException e) {
            return Map.of();
        }
    }
}
