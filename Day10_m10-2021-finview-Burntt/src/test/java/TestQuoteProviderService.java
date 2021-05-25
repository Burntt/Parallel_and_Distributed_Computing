import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestQuoteProviderService {
    private AtomicInteger calls;
    private QuoteLibrary ql;
    private QuoteProviderService qps;

    @BeforeEach
    void start() {
        calls = new AtomicInteger();
        ql = new QuoteLibrary() {
            private final QuoteLibrary impl = new QuoteLibraryImpl(3.0, 0.03, 0.09);
            @Override
            public void registerUserThread() {
                impl.registerUserThread();
            }
            @Override
            public Quote.Tick getTick(String ticker) {
                calls.incrementAndGet();
                return impl.getTick(ticker);
            }
        };
        qps = ReplacesUtils.instance(QuoteProviderServiceImpl.class, ql, "SPY");
    }

    @AfterEach
    void stop() {
        calls = null;
        ql = null;
        qps.shutdownAndAwaitTermination();
        qps = null;
    }

    @Test
    void testSimple() {
        long beginNs, endNs;

        beginNs = System.nanoTime();
        CompletableFuture<Quote> f1 = qps.getLatestAndPreviousTicks();
        endNs = System.nanoTime();
        Quote v1 = f1.join();

        assertTrue((endNs - beginNs) < 10_000_000, "call must last less than 10ms");
        assertEquals("SPY", v1.getTicker());
        assertEquals(1, v1.getTicks().size());
        assertEquals(1, calls.get());

        beginNs = System.nanoTime();
        CompletableFuture<Quote> f2 = qps.getLatestAndPreviousTicks();
        endNs = System.nanoTime();
        Quote v2 = f2.join();

        assertTrue((endNs - beginNs) < 10_000_000, "call must last less than 10ms");
        assertEquals("SPY", v2.getTicker());
        assertEquals(2, v2.getTicks().size());
        assertEquals(2, calls.get());

        assertNotEquals(v1.getTicks().get(0), v2.getTicks().get(1));
        assertEquals(v1.getTicks().get(0), v2.getTicks().get(0));

        beginNs = System.nanoTime();
        CompletableFuture<Quote> f3 = qps.getLatestAndPreviousTicks();
        endNs = System.nanoTime();
        Quote v3 = f3.join();

        assertTrue((endNs - beginNs) < 10_000_000, "call must last less than 10ms");
        assertEquals("SPY", v3.getTicker());
        assertEquals(2, v3.getTicks().size());
        assertEquals(3, calls.get());

        assertNotEquals(v1.getTicks().get(0), v3.getTicks().get(0));
        assertNotEquals(v1.getTicks().get(0), v3.getTicks().get(1));
        assertNotEquals(v2.getTicks().get(1), v3.getTicks().get(1));
        assertEquals(v2.getTicks().get(1), v3.getTicks().get(0));

        beginNs = System.nanoTime();
        CompletableFuture<Quote> f4 = qps.getLatestAndPreviousTicks();
        endNs = System.nanoTime();
        Quote v4 = f4.join();

        assertTrue((endNs - beginNs) < 10_000_000, "call must last less than 10ms");
        assertEquals("SPY", v4.getTicker());
        assertEquals(2, v4.getTicks().size());
        assertEquals(4, calls.get());

        assertNotEquals(v1.getTicks().get(0), v4.getTicks().get(0));
        assertNotEquals(v1.getTicks().get(0), v4.getTicks().get(1));
        assertNotEquals(v2.getTicks().get(1), v4.getTicks().get(0));
        assertNotEquals(v2.getTicks().get(1), v4.getTicks().get(1));
        assertNotEquals(v3.getTicks().get(1), v4.getTicks().get(1));
        assertEquals(v3.getTicks().get(1), v4.getTicks().get(0));
    }

    @Test
    void testThreaded() throws InterruptedException {
        int workerCount = 12;
        List<Thread> workers = new LinkedList<>();
        AtomicInteger stalls = new AtomicInteger();
        CountDownLatch prepare = new CountDownLatch(workerCount);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < workerCount; ++i) {
            Thread worker = new Thread(() -> {
                prepare.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                }

                long beginNs = System.nanoTime();
                CompletableFuture<Quote> f = qps.getLatestAndPreviousTicks();
                long endNs = System.nanoTime();

                if (endNs - beginNs > 10_000_000) {
                    stalls.incrementAndGet();
                }

                f.join();
            });
            worker.start();
            workers.add(worker);
        }
        prepare.await();
        start.countDown();
        for (Thread worker : workers) {
            worker.join();
        }
        assertEquals(workerCount, calls.get());
        assertEquals(0, stalls.get(), "there were long calls");
    }
}
