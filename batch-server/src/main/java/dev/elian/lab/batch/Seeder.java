package dev.elian.lab.batch;

import dev.elian.lab.common.Knobs;
import dev.elian.lab.common.SendMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 매 variant 를 같은 출발선에서 시작시킨다. 데이터 초기화만 한다. */
@Component
public class Seeder {

    private final JdbcTemplate jdbc;

    public Seeder(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void reset(Knobs k, int tenants, int accountsPerTenant, int requests) {
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String t : List.of("notification", "notification_request", "member_account", "tenant")) {
            jdbc.execute("TRUNCATE TABLE " + t);
        }
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");

        for (int h = 1; h <= tenants; h++) {
            jdbc.update("INSERT INTO tenant (id, name) VALUES (?, ?)", h, "테넌트" + h);
        }
        List<Object[]> accounts = new ArrayList<>();
        int accountId = 0;
        for (int h = 1; h <= tenants; h++) {
            for (int a = 0; a < accountsPerTenant; a++) accounts.add(new Object[]{++accountId, h});
        }
        jdbc.batchUpdate("INSERT INTO member_account (id, tenant_id) VALUES (?, ?)", accounts);

        int totalAccounts = accountId;
        List<Object[]> reqs = new ArrayList<>(requests);
        for (int i = 0; i < requests; i++) {
            long acc = 1 + (i % totalAccounts);
            long hos = 1 + ((acc - 1) / accountsPerTenant);
            reqs.add(new Object[]{hos, acc, "payload-" + i});
        }
        jdbc.batchUpdate("""
                INSERT INTO notification_request (tenant_id, account_id, payload, status, requested_at)
                VALUES (?, ?, ?, 'PENDING', NOW(6))
                """, reqs);

        if (k.sendMode() == SendMode.UPDATE_ONLY) {
            // 알림 행을 미리 만들어 둔다 → 배치는 INSERT 없이 상태만 UPDATE 하게 된다
            jdbc.update("""
                    INSERT INTO notification (account_id, request_id, status, created_at)
                    SELECT account_id, id, 'CREATED', NOW(6) FROM notification_request
                    """);
        }
    }

    /** V8 검증용 — FK 를 붙였다 뗀다. */
    public void setForeignKey(boolean enabled) {
        try {
            if (enabled) {
                jdbc.execute("ALTER TABLE notification ADD CONSTRAINT fk_noti_account "
                           + "FOREIGN KEY (account_id) REFERENCES member_account (id)");
            } else {
                jdbc.execute("ALTER TABLE notification DROP FOREIGN KEY fk_noti_account");
            }
        } catch (RuntimeException ignored) { /* 이미 그 상태 */ }
    }

    public long pending() {
        Long v = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_request WHERE status = 'PENDING'", Long.class);
        return v == null ? 0 : v;
    }
}
