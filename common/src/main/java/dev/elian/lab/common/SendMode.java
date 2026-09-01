package dev.elian.lab.common;

/** 발송 방식. 운영에서 데드락을 잡으려고 셋 다 시도했다. */
public enum SendMode {
    /** 건별 INSERT + UPDATE. 최초 상태 */
    PER_ROW,
    /** chunk 단위로 묶어 벌크 실행 */
    BULK,
    /**
     * 알림 행을 미리 만들어 두고 배치는 상태만 UPDATE.
     * <b>INSERT 가 없으므로 FK 부모 행 S락이 걸리지 않는다.</b>
     */
    UPDATE_ONLY
}
