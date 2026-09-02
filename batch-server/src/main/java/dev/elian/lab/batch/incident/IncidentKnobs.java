package dev.elian.lab.batch.incident;

/**
 * 2026-xx 공지 푸시 사건 재현 노브.
 *
 * <p>사건 요약 — 배치가 콘텐츠 3건을 한 트랜잭션으로 처리했다.
 * 첫 콘텐츠는 자기 발송이 끝난 시점에 완료 표시(UPDATE)를 했지만,
 * 커밋은 뒤따르는 두 건의 발송까지 전부 끝난 뒤였다. 그래서 그 행이 99초 잠겼다.
 * <b>UPDATE 자체는 수 ms다. 오래 잠근 게 아니라 안 풀어준 것이다.</b>
 *
 * <p>그사이 푸시를 받은 사용자가 콘텐츠를 열면 조회수 UPDATE 가 같은 행을 기다린다.
 * 본문 조회는 이미 끝나 있는데, 조회수 증가가 같은 트랜잭션에 있어서 응답 전체가 실패한다.
 */
public record IncidentKnobs(
        String id,
        String label,
        /** 완료 표시를 콘텐츠마다 즉시 커밋하는가 (사건 당시 = false) */
        boolean commitPerContent,
        /** 조회수 증가가 본문 조회와 같은 트랜잭션인가 (사건 당시 = true) */
        boolean viewInSameTx,
        /** 커밋을 마친 뒤에 푸시를 보내는가 (사건 당시 = false — 푸시가 먼저 나갔다) */
        boolean commitBeforePush,
        int lockWaitTimeoutSec
) {
    /** 사건 당시 구성 */
    public static IncidentKnobs asHappened() {
        return new IncidentKnobs("AS-IS", "사건 당시", false, true, false, 50);
    }

    public IncidentKnobs id(String id, String label) {
        return new IncidentKnobs(id, label, commitPerContent, viewInSameTx, commitBeforePush, lockWaitTimeoutSec);
    }
    public IncidentKnobs withCommitPerContent() { return new IncidentKnobs(id, label, true, viewInSameTx, commitBeforePush, lockWaitTimeoutSec); }
    public IncidentKnobs viewSeparateTx()   { return new IncidentKnobs(id, label, commitPerContent, false, commitBeforePush, lockWaitTimeoutSec); }
    /** 커밋 후 푸시 — 완료 표시가 실제로 커밋돼 있어야 성립하므로 건별 커밋을 함께 켠다. */
    public IncidentKnobs commitThenPush()   { return new IncidentKnobs(id, label, true, viewInSameTx, true, lockWaitTimeoutSec); }
    public IncidentKnobs lockWait(int sec)  { return new IncidentKnobs(id, label, commitPerContent, viewInSameTx, commitBeforePush, sec); }

    public String describe() {
        return "완료표시 %s / 조회수 %s / %s / lock_wait %ds".formatted(
                commitPerContent ? "건별 커밋" : "일괄 커밋",
                viewInSameTx ? "본문과 같은 TX" : "분리",
                commitBeforePush ? "커밋 후 푸시" : "푸시 후 커밋",
                lockWaitTimeoutSec);
    }
}
