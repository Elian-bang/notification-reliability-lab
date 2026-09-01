package dev.elian.lab.common;

/**
 * 실험 노브. baseline 에서 <b>하나씩만</b> 바꾼다.
 *
 * <p>토폴로지: 배치(알림 A → FK로 계정 B) ↔ 수신자(계정 B → 알림 A).
 * 배치는 member_account 를 갱신하지 않는다. 계정 락은 FK 로만 걸린다.
 */
public record Knobs(
        String id,
        String label,

        // ── 배치 쪽 ──
        JobShape shape,          // Tasklet 인가 chunk 인가
        int chunkSize,           // CHUNK 일 때 chunk 크기 = 트랜잭션 단위
        boolean queryInTx,       // 대상 조회가 트랜잭션 안인가
        SendMode sendMode,       // 건별 / 벌크 / 상태만 UPDATE
        boolean apiCallInTx,     // 외부 채널사 호출이 트랜잭션 안인가

        // ── 수신자 쪽 ──
        boolean receiverUnify,   // 수신자도 A→B 순서로 (배치와 통일)
        boolean receiverTxSplit, // 토큰 갱신 / 알림 확인을 별도 트랜잭션으로

        // ── 스키마·DB ──
        boolean foreignKey,      // notification → member_account FK 유지
        String isolation
) {
    public static final String RR = "REPEATABLE READ";
    public static final String RC = "READ COMMITTED";

    /** ① 운영 최초 상태 — Tasklet, 조회 TX 안, 건별 INSERT, API 호출도 TX 안 */
    public static Knobs v0() {
        return new Knobs("V0", "① Tasklet (최초)",
                JobShape.TASKLET, 500, true, SendMode.PER_ROW, true,
                false, false, true, RR);
    }

    public Knobs id(String id, String label) {
        return new Knobs(id, label, shape, chunkSize, queryInTx, sendMode, apiCallInTx,
                receiverUnify, receiverTxSplit, foreignKey, isolation);
    }
    public Knobs shape(JobShape s)      { return new Knobs(id, label, s, chunkSize, queryInTx, sendMode, apiCallInTx, receiverUnify, receiverTxSplit, foreignKey, isolation); }
    public Knobs chunkSize(int n)       { return new Knobs(id, label, shape, n, queryInTx, sendMode, apiCallInTx, receiverUnify, receiverTxSplit, foreignKey, isolation); }
    public Knobs queryOutsideTx()       { return new Knobs(id, label, shape, chunkSize, false, sendMode, apiCallInTx, receiverUnify, receiverTxSplit, foreignKey, isolation); }
    public Knobs sendMode(SendMode m)   { return new Knobs(id, label, shape, chunkSize, queryInTx, m, apiCallInTx, receiverUnify, receiverTxSplit, foreignKey, isolation); }
    public Knobs apiCallOutsideTx()     { return new Knobs(id, label, shape, chunkSize, queryInTx, sendMode, false, receiverUnify, receiverTxSplit, foreignKey, isolation); }
    public Knobs withReceiverUnify()    { return new Knobs(id, label, shape, chunkSize, queryInTx, sendMode, apiCallInTx, true, receiverTxSplit, foreignKey, isolation); }
    public Knobs withReceiverSplitTx()  { return new Knobs(id, label, shape, chunkSize, queryInTx, sendMode, apiCallInTx, receiverUnify, true, foreignKey, isolation); }
    public Knobs noForeignKey()         { return new Knobs(id, label, shape, chunkSize, queryInTx, sendMode, apiCallInTx, receiverUnify, receiverTxSplit, false, isolation); }
    public Knobs isolation(String i)    { return new Knobs(id, label, shape, chunkSize, queryInTx, sendMode, apiCallInTx, receiverUnify, receiverTxSplit, foreignKey, i); }

    public String describe() {
        return "%s%s / 조회 %s / 발송 %s / API %s / 수신자 %s%s / FK %s / %s".formatted(
                shape,
                shape == JobShape.CHUNK ? "(" + chunkSize + ")" : "",
                queryInTx ? "TX내" : "TX밖",
                switch (sendMode) { case PER_ROW -> "건별"; case BULK -> "벌크"; case UPDATE_ONLY -> "상태만UPDATE"; },
                apiCallInTx ? "TX내" : "TX밖",
                receiverUnify ? "A→B" : "B→A",
                receiverTxSplit ? "+TX분리" : "",
                foreignKey ? "O" : "X",
                isolation);
    }
}
