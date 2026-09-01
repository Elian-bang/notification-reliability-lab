package dev.elian.lab.batch;

/** 발송 대상 한 건. reader 가 내보내고 writer 가 받는다. */
public record SendTarget(long requestId, long accountId) {}
