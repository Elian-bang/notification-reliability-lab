package dev.elian.lab.common;

/**
 * 실험 구성. baseline(V0)에서 노브를 <b>하나씩만</b> 바꾼다.
 *
 * <p>토폴로지: 배치(A -> FK로 B) vs 수신자(B -> A).
 * A = notification, B = member_account.
 * <b>배치는 member_account 를 갱신하지 않는다.</b> 계정 락은 오직 FK 로만 걸린다.
 */
public record Variant(
        String id,
        String label,
        boolean queryInsideTx,     // 배치 대상 조회가 트랜잭션 안인가 (운영 원본 = true)
        int chunkSize,             // Spring Batch chunk = 트랜잭션 단위
        boolean batchUpdate,       // 상태 갱신을 JDBC batch 로 묶는가
        boolean receiverUnify,     // 수신자도 A->B 로 (V4)
        boolean receiverTxSplit,   // 수신자 토큰갱신/알림확인 트랜잭션 분리 (V9)
        boolean foreignKey,        // notification -> member_account FK 유지 (V8 에서 false)
        String isolation
) {
    public static final String RR = "REPEATABLE READ";
    public static final String RC = "READ COMMITTED";

    public static Variant v0() {
        return new Variant("V0", "baseline (사건 재현)", true, 500, false, false, false, true, RR);
    }

    public Variant queryOutside()  { return new Variant(id, label, false, chunkSize, batchUpdate, receiverUnify, receiverTxSplit, foreignKey, isolation); }
    public Variant chunk(int n)    { return new Variant(id, label, queryInsideTx, n, batchUpdate, receiverUnify, receiverTxSplit, foreignKey, isolation); }
    public Variant batch()         { return new Variant(id, label, queryInsideTx, chunkSize, true, receiverUnify, receiverTxSplit, foreignKey, isolation); }
    public Variant unifyOrder()    { return new Variant(id, label, queryInsideTx, chunkSize, batchUpdate, true, receiverTxSplit, foreignKey, isolation); }
    public Variant splitTx()       { return new Variant(id, label, queryInsideTx, chunkSize, batchUpdate, receiverUnify, true, foreignKey, isolation); }
    public Variant noForeignKey()  { return new Variant(id, label, queryInsideTx, chunkSize, batchUpdate, receiverUnify, receiverTxSplit, false, isolation); }
    public Variant isolation(String i) { return new Variant(id, label, queryInsideTx, chunkSize, batchUpdate, receiverUnify, receiverTxSplit, foreignKey, i); }
    public Variant name(String i, String l) { return new Variant(i, l, queryInsideTx, chunkSize, batchUpdate, receiverUnify, receiverTxSplit, foreignKey, isolation); }

    public String describe() {
        return "조회 %s / chunk %d / %s / 수신자 %s%s / FK %s / %s".formatted(
                queryInsideTx ? "TX내" : "TX밖",
                chunkSize,
                batchUpdate ? "BATCH UPDATE" : "개별 UPDATE",
                receiverUnify ? "A->B(통일)" : "B->A(반대)",
                receiverTxSplit ? " TX분리" : "",
                foreignKey ? "있음" : "없음",
                isolation);
    }
}
