package dev.elian.lab.batch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 트랜잭션 하나가 얼마나 오래 열려 있었는지 잰다.
 *
 * <p>chunk 크기를 정하는 문제의 핵심은 <b>커밋 횟수와 락 보유 시간의 맞교환</b>이다.
 * chunk 를 키우면 커밋이 줄어 처리량은 오르지만 트랜잭션이 길어져 락을 오래 쥔다.
 * 그 균형점을 찾으려면 트랜잭션 지속시간을 직접 재야 한다.
 */
public class TxTimer {

    private final ConcurrentLinkedQueue<Long> durationsMicros = new ConcurrentLinkedQueue<>();
    private final ThreadLocal<Long> started = new ThreadLocal<>();

    public void begin() { started.set(System.nanoTime()); }

    public void end() {
        Long t0 = started.get();
        if (t0 == null) return;
        durationsMicros.add((System.nanoTime() - t0) / 1000);
        started.remove();
    }

    public int count() { return durationsMicros.size(); }

    public double meanMillis() {
        List<Long> all = new ArrayList<>(durationsMicros);
        if (all.isEmpty()) return 0;
        return all.stream().mapToLong(Long::longValue).average().orElse(0) / 1000.0;
    }

    public double percentileMillis(double p) {
        List<Long> all = new ArrayList<>(durationsMicros);
        if (all.isEmpty()) return 0;
        Collections.sort(all);
        int i = (int) Math.ceil(p / 100.0 * all.size()) - 1;
        return all.get(Math.max(0, Math.min(i, all.size() - 1))) / 1000.0;
    }

    public double maxMillis() {
        return new ArrayList<>(durationsMicros).stream().mapToLong(Long::longValue).max().orElse(0) / 1000.0;
    }
}
