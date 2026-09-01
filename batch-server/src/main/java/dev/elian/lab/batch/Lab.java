package dev.elian.lab.batch;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;

/** 공용 유틸 — 에러코드 판별, InnoDB 관측. */
public final class Lab {
    private Lab() {}

    /**
     * Spring 예외 타입에 의존하지 않는다.
     * 1213(데드락)과 1205(락 타임아웃)이 둘 다 PessimisticLockingFailureException 하위라
     * 타입만으로는 구분되지 않는다. 2판 첫 실행에서 데드락 137건이 전부 타임아웃으로
     * 잘못 집계된 실제 사고가 있었다.
     */
    public static int mysqlErrorCode(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof SQLException se) return se.getErrorCode();
        }
        return -1;
    }

    public static long innodbDeadlocks(JdbcTemplate jdbc) {
        try {
            Long v = jdbc.queryForObject(
                    "SELECT COUNT FROM information_schema.INNODB_METRICS WHERE NAME = 'lock_deadlocks'",
                    Long.class);
            return v == null ? -1 : v;
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** SHOW ENGINE INNODB STATUS 에서 LATEST DETECTED DEADLOCK 구간만 잘라낸다. */
    public static String latestDeadlock(JdbcTemplate jdbc) {
        try {
            String status = jdbc.query("SHOW ENGINE INNODB STATUS",
                    rs -> rs.next() ? rs.getString("Status") : "");
            if (status == null) return "";
            int s = status.indexOf("LATEST DETECTED DEADLOCK");
            if (s < 0) return "";
            int e = status.indexOf("TRANSACTIONS", s);
            return status.substring(s, e > s ? e : Math.min(status.length(), s + 8000));
        } catch (RuntimeException ex) {
            return "(캡처 실패: " + ex.getMessage() + ")";
        }
    }
}
