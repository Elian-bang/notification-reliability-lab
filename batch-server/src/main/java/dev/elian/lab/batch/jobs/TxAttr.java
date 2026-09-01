package dev.elian.lab.batch.jobs;

import dev.elian.lab.common.Knobs;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

/**
 * chunk / tasklet 트랜잭션에 격리수준을 실제로 건다.
 *
 * <p>1차 측정에서 V5(READ COMMITTED)가 수신자에만 적용되고 배치에는 안 걸려 반쪽이었다.
 * 트랜잭션 매니저 충돌을 고치는 과정에서 빠졌던 것.
 */
final class TxAttr {
    private TxAttr() {}

    static DefaultTransactionAttribute of(Knobs k) {
        DefaultTransactionAttribute a = new DefaultTransactionAttribute();
        a.setIsolationLevel(Knobs.RC.equals(k.isolation())
                ? TransactionDefinition.ISOLATION_READ_COMMITTED
                : TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return a;
    }
}
