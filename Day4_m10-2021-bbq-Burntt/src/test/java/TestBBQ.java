import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.lang.Thread.State;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestBBQ {
    BBQ instance(int capacity) {
        return ReplacesUtils.instance(BBQImpl.class, capacity);
    }

    @Test
    public void testSimple() throws InterruptedException {
        BBQ q = instance(2);
        q.put("hello");
        q.put("world");
        assertEquals("hello", q.take());
        assertEquals("world", q.take());
    }

    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    @ParameterizedTest(name = "{displayName} capacity {argumentsWithNames}")
    @ValueSource(ints = {1, 4})
    public void testPutBlocks(int capacity) throws InterruptedException {
        BBQ q = instance(capacity);
        for (int i = 0; i < capacity; ++i) {
            q.put("hello");
        }
        Thread t = spawnThread(() -> {
            try {
                q.put("world");
            } catch (InterruptedException e) {
            }
        });
        Thread.sleep(100);
        State s = t.getState();
        if (s != Thread.State.WAITING && s != Thread.State.TIMED_WAITING) {
            fail(String.format("bad thread state; expected WAITING or TIMED_WAITING; got %s", s));
        }
        for (int i = 0; i < capacity; ++i) {
            assertEquals("hello", q.take());
        }
        assertEquals("world", q.take());
        joinAllThreads();
    }

    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    @ParameterizedTest(name = "{displayName} capacity {argumentsWithNames}")
    @ValueSource(ints = {1, 4})
    public void testTakeBlocks(int capacity) throws InterruptedException {
        BBQ q = instance(capacity);
        Thread t = spawnThread(() -> {
            try {
                q.take();
            } catch (InterruptedException e) {
            }
        });
        Thread.sleep(100);
        State s = t.getState();
        if (s != Thread.State.WAITING && s != Thread.State.TIMED_WAITING) {
            fail(String.format("bad thread state; expected WAITING or TIMED_WAITING; got %s", s));
        }
        q.put("hello");
        joinAllThreads();
    }

    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    @ParameterizedTest(name = "{displayName} capacity {argumentsWithNames}")
    @ValueSource(ints = {1, 4})
    public void testTwoThreadsPassingObjects(int capacity) throws InterruptedException {
        BBQ q = instance(capacity);
        int limit = 100_000;
        ObjectProducerRunnable producer = new ObjectProducerRunnable(q, limit);
        ObjectConsumerRunnable consumer = new ObjectConsumerRunnable(q, limit);
        spawnThread(producer);
        spawnThread(consumer);
        joinAllThreads();
        assertEquals(limit, producer.getCount());
        assertEquals(limit, consumer.getCount());
    }

    static class ObjectProducerRunnable implements Runnable {
        private final BBQ queue;
        private int count;
        private int limit;

        ObjectProducerRunnable(BBQ q, int l) {
            queue = q;
            count = 0;
            limit = l;
        }

        public void run() {
            try {
                while (count < limit && !Thread.interrupted()) {
                    queue.put(new Object());
                    count++;
                }
            } catch (InterruptedException ignored) {
            }
        }

        public int getCount() {
            return count;
        }
    }

    static class ObjectConsumerRunnable implements Runnable {
        private final BBQ queue;
        private int count;
        private int limit;

        ObjectConsumerRunnable(BBQ q, int l) {
            queue = q;
            count = 0;
            limit = l;
        }

        public void run() {
            try {
                while (count < limit && !Thread.interrupted()) {
                    queue.take();
                    count++;
                }
            } catch (InterruptedException ignored) {
            }
        }

        public int getCount() {
            return count;
        }
    }

    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    @ParameterizedTest(name = "{displayName} capacity {argumentsWithNames}")
    @ValueSource(ints = {1, 4})
    public void testSixteenThreadsPassingIntegers(int capacity) {
        BBQ q = instance(capacity);
        int limit = 100_000;
        int producerThreadCount = 4;
        int consumerThreadCount = 4;
        List<AtomicLong> producerChecksums = new LinkedList<>();
        List<AtomicLong> consumerChecksums = new LinkedList<>();
        AtomicLong producerErrorCount = new AtomicLong();
        AtomicLong consumerErrorCount = new AtomicLong();
        // Setup producers.
        for (int i = 0; i < producerThreadCount; ++i) {
            AtomicLong producerChecksum = new AtomicLong();
            producerChecksums.add(producerChecksum);
            spawnThread(() -> {
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                int count = 0;
                long checksum = 0;
                while (count < limit && !Thread.interrupted()) {
                    long value = rng.nextLong();
                    checksum ^= value;
                    try {
                        q.put(value);
                    } catch (InterruptedException e) {
                        producerErrorCount.incrementAndGet();
                    }
                    ++count;
                }
                producerChecksum.set(checksum);
            });
        }
        // Setup consumers.
        for (int i = 0; i < consumerThreadCount; ++i) {
            AtomicLong consumerChecksum = new AtomicLong();
            consumerChecksums.add(consumerChecksum);
            spawnThread(() -> {
                int count = 0;
                long checksum = 0;
                while (count < limit && !Thread.interrupted()) {
                    Object item = null;
                    try {
                        item = q.take();
                    } catch (InterruptedException e) {
                        consumerErrorCount.incrementAndGet();
                    }
                    if (item != null && item instanceof Long) {
                        checksum ^= (Long) item;
                    } else {
                        consumerErrorCount.incrementAndGet();
                    }
                    ++count;
                }
                consumerChecksum.set(checksum);
            });
        }
        // Run the test.
        joinAllThreads();
        // Check that there were no errors.
        assertEquals(0, producerErrorCount.get());
        assertEquals(0, consumerErrorCount.get());
        // Check the checksums.
        long finalChecksum = 0;
        for (AtomicLong c : producerChecksums) {
            finalChecksum ^= c.get();
        }
        for (AtomicLong c : consumerChecksums) {
            finalChecksum ^= c.get();
        }
        assertEquals(0, finalChecksum);
    }

    private static final long TIMEOUT_MILLIS = 15_000;

    private ThreadGroup threadGroup;
    private List<Thread> threadList;

    @BeforeEach
    private void beforeCallback() {
        threadGroup = new ThreadGroup("children");
        threadList = new LinkedList<>();
    }

    @AfterEach
    private void afterCallback() {
        threadList.forEach(Thread::stop);
        threadGroup.stop();
        joinAllThreads();
        threadList.clear();
        threadGroup.destroy();
    }

    private Thread spawnThread(Runnable target) {
        Thread thread = new Thread(threadGroup, target);
        thread.start();
        threadList.add(thread);
        return thread;
    }

    private void joinAllThreads() {
        boolean timeout = false;
        for (Thread thread : threadList) {
            try {
                thread.join(TIMEOUT_MILLIS);
            } catch (InterruptedException e) {
                thread.stop();
            }
            if (thread.isAlive()) {
                timeout = true;
            }
        }
        if (timeout) {
            fail("test timed out while waiting for a thread to complete");
        }
    }
}
