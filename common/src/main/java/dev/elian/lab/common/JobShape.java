package dev.elian.lab.common;

/** 배치 구현 형태. 운영에서 ① Tasklet → ② chunk 순서로 갔다. */
public enum JobShape {
    /**
     * ① Tasklet — 건별로 트랜잭션을 따로 연다 (REQUIRES_NEW).
     *
     * <p>운영 최초 구조가 이것이었다. <b>트랜잭션이 아주 짧은데도 데드락이 났다.</b>
     * 트랜잭션 길이가 원인이 아니라는 직접 증거다.
     */
    PER_REQUEST,
    /** Tasklet 이지만 여러 건을 한 트랜잭션에 묶은 형태 (곡선의 한쪽 끝) */
    TASKLET,
    /** ② Spring Batch chunk-oriented. chunk 하나가 트랜잭션 하나 */
    CHUNK
}
