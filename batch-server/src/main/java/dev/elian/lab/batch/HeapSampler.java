package dev.elian.lab.batch;

/**
 * 실행 중 힙 사용량 최댓값을 샘플링한다.
 *
 * <p>chunk 를 키우면 한 트랜잭션이 들고 있는 객체가 늘어난다.
 * 처리량·락과 함께 <b>메모리도 같은 노브에 매달려 있다</b>는 것을 수치로 보이기 위해 잰다.
 */
public class HeapSampler implements AutoCloseable {

    private final Thread thread;
    private volatile boolean running = true;
    private volatile long peakBytes = 0;

    public HeapSampler() {
        thread = new Thread(() -> {
            Runtime rt = Runtime.getRuntime();
            while (running) {
                long used = rt.totalMemory() - rt.freeMemory();
                if (used > peakBytes) peakBytes = used;
                try { Thread.sleep(40); } catch (InterruptedException e) { return; }
            }
        }, "heap-sampler");
        thread.setDaemon(true);
        thread.start();
    }

    public long peakMb() { return peakBytes / (1024 * 1024); }

    @Override public void close() { running = false; thread.interrupt(); }
}
