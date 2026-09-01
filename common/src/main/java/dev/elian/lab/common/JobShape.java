package dev.elian.lab.common;

/** 배치 구현 형태. 운영에서 ① Tasklet → ② chunk 순서로 갔다. */
public enum JobShape {
    /** ① 한 트랜잭션에서 대상 전체를 통째로 처리 */
    TASKLET,
    /** ② Spring Batch chunk-oriented. chunk 하나가 트랜잭션 하나 */
    CHUNK
}
