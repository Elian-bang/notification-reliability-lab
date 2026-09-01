package dev.elian.lab.batch;

import dev.elian.lab.common.Variant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 서버 3개를 DB 로 동기화한다.
 * 배치가 control 을 쓰고, Node 쪽(수신자·요청)이 폴링해서 자기 부하를 켜고 끈다.
 * HTTP 조율 대신 DB 를 쓴 이유: 의존성이 없고, 어차피 셋 다 같은 DB 를 본다.
 */
@Component
public class Coordinator {

    private final JdbcTemplate jdbc;

    public Coordinator(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void activate(String runId, Variant v) {
        jdbc.update("""
                INSERT INTO experiment_control
                    (id, run_id, variant_id, receiver_unify, receiver_tx_split, isolation_level, active, updated_at)
                VALUES (1, ?, ?, ?, ?, ?, 1, NOW(6))
                ON DUPLICATE KEY UPDATE
                    run_id=VALUES(run_id), variant_id=VALUES(variant_id),
                    receiver_unify=VALUES(receiver_unify), receiver_tx_split=VALUES(receiver_tx_split),
                    isolation_level=VALUES(isolation_level), active=1, updated_at=NOW(6)
                """,
                runId, v.id(), v.receiverUnify() ? 1 : 0, v.receiverTxSplit() ? 1 : 0, v.isolation());
    }

    public void deactivate() {
        jdbc.update("UPDATE experiment_control SET active = 0, updated_at = NOW(6) WHERE id = 1");
    }

    /** Node 쪽이 올려둔 지표를 읽는다. 없으면 빈 맵. */
    public Map<String, Object> readMetric(String runId, String role) {
        try {
            return jdbc.queryForMap(
                    "SELECT * FROM experiment_metric WHERE run_id = ? AND role = ?", runId, role);
        } catch (RuntimeException e) {
            return Map.of();
        }
    }


    /** V8 검증용 — FK 를 붙였다 뗀다. */
    public void setForeignKey(boolean enabled) {
        if (enabled) {
            try {
                jdbc.execute("""
                        ALTER TABLE notification
                        ADD CONSTRAINT fk_noti_account FOREIGN KEY (account_id) REFERENCES member_account (id)
                        """);
            } catch (RuntimeException ignored) { /* 이미 있음 */ }
        } else {
            try {
                jdbc.execute("ALTER TABLE notification DROP FOREIGN KEY fk_noti_account");
            } catch (RuntimeException ignored) { /* 이미 없음 */ }
        }
    }

    /** 매 variant 마다 같은 출발선에서 시작한다. */
    public void resetData(int tenants, int accountsPerTenant, int requests) {
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbc.execute("TRUNCATE TABLE notification");
        jdbc.execute("TRUNCATE TABLE notification_request");
        jdbc.execute("TRUNCATE TABLE member_account");
        jdbc.execute("TRUNCATE TABLE tenant");
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");

        for (int h = 1; h <= tenants; h++) {
            jdbc.update("INSERT INTO tenant (id, name) VALUES (?, ?)", h, "테넌트" + h);
        }
        int accountId = 0;
        for (int h = 1; h <= tenants; h++) {
            for (int a = 0; a < accountsPerTenant; a++) {
                jdbc.update("INSERT INTO member_account (id, tenant_id) VALUES (?, ?)", ++accountId, h);
            }
        }
        final int totalAccounts = accountId;
        java.util.List<Object[]> rows = new java.util.ArrayList<>(requests);
        for (int i = 0; i < requests; i++) {
            long acc = 1 + (i % totalAccounts);
            long hos = 1 + ((acc - 1) / accountsPerTenant);
            rows.add(new Object[]{hos, acc, "payload-" + i});
        }
        jdbc.batchUpdate("""
                INSERT INTO notification_request (tenant_id, account_id, payload, status, requested_at)
                VALUES (?, ?, ?, 'PENDING', NOW(6))
                """, rows);
    }

    public long pendingRequests() {
        Long v = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_request WHERE status = 'PENDING'", Long.class);
        return v == null ? 0 : v;
    }
}
