import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class TestRWLock {
    RWLock instance() {
        return ReplacesUtils.instance(RWLockImpl.class);
    }

    @Test
    public void testSimple() throws InterruptedException {
        RWLock l = instance();
        l.readLock();
        l.readUnlock();
        l.writeLock();
        l.writeUnlock();
    }

    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    @Test
    public void testReadersBlockWriters() throws InterruptedException {
        AtomicLong step = new AtomicLong();
        RWLock l = instance();
        l.readLock();
        step.set(0);
        Thread t = spawnThread(() -> {
            step.set(1);
            try {
                l.writeLock();
            } catch (InterruptedException e) {
            } finally {
                l.writeUnlock();
                step.set(2);
            }
        });
        waitFor(() -> step.get() == 1 && threadIsWaiting(t), "writer thread is waiting");
        l.readUnlock();
        waitFor(() -> step.get() == 2, "writer acquired and released the lock");
        joinAllThreads();
    }

    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    @Test
    public void testWritersBlockReaders() throws InterruptedException {
        AtomicLong step = new AtomicLong();
        RWLock l = instance();
        l.writeLock();
        step.set(0);
        Thread t = spawnThread(() -> {
            step.set(1);
            try {
                l.readLock();
            } catch (InterruptedException e) {
            } finally {
                l.readUnlock();
                step.set(2);
            }
        });
        waitFor(() -> step.get() == 1 && threadIsWaiting(t), "reader thread is waiting");
        l.writeUnlock();
        waitFor(() -> step.get() == 2, "reader acquired and released the lock");
        joinAllThreads();
    }

    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    @Test
    public void testReadersMayGoConcurrently() throws InterruptedException {
        AtomicLong acquires = new AtomicLong();
        AtomicLong releases = new AtomicLong();
        CountDownLatch latch = new CountDownLatch(1);
        RWLock l = instance();
        Runnable readerRunnable = () -> {
            try {
                l.readLock();
                acquires.incrementAndGet();
                latch.await();
            } catch (InterruptedException e) {
            } finally {
                l.readUnlock();
                releases.incrementAndGet();
            }
        };
        Thread r1 = spawnThread(readerRunnable);
        Thread r2 = spawnThread(readerRunnable);
        waitFor(() -> acquires.get() - releases.get() == 2 && threadIsWaiting(r1)
                && threadIsWaiting(r2), "readers acquired the lock");
        latch.countDown();
        waitFor(() -> acquires.get() - releases.get() == 0, "readers released the lock");
        joinAllThreads();
    }

    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    @Test
    public void testWritersMustGoSerial() throws InterruptedException {
        AtomicLong acquires = new AtomicLong();
        AtomicLong releases = new AtomicLong();
        CountDownLatch latch = new CountDownLatch(1);
        RWLock l = instance();
        Runnable writerRunnable = () -> {
            try {
                l.writeLock();
                acquires.incrementAndGet();
                latch.await();
            } catch (InterruptedException e) {
            } finally {
                l.writeUnlock();
                releases.incrementAndGet();
            }
        };

        Thread r1 = spawnThread(writerRunnable);
        Thread r2 = spawnThread(writerRunnable);

        waitFor(() -> acquires.get() == 1 && releases.get() == 0 && threadIsWaiting(r1)
                && threadIsWaiting(r2), "one writer acquired the lock, one writer is waiting");
        latch.countDown();
        waitFor(() -> acquires.get() == 2 && releases.get() == 2,
                "writers acquired and released the lock");
        joinAllThreads();
    }

    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    @Test
    public void testStress() throws InterruptedException {
        int iterations = 100_000;
        int threads = 8;
        AtomicLong state = new AtomicLong();
        AtomicLong errors = new AtomicLong();
        CountDownLatch start = new CountDownLatch(1);
        RWLock l = instance();
        Runnable writerRunnable = () -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                return;
            }
            for (int i = 0; i < iterations; ++i) {
                try {
                    l.writeLock();
                    long value;
                    value = state.addAndGet(0xffff);
                    if (value != 0xffff) {
                        errors.incrementAndGet();
                    }
                    value = state.addAndGet(-0xffff);
                    if (value != 0) {
                        errors.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    return;
                } finally {
                    l.writeUnlock();
                }
            }
        };
        Runnable readerRunnable = () -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                return;
            }
            for (int i = 0; i < iterations; ++i) {
                try {
                    l.readLock();
                    long value;
                    value = state.incrementAndGet();
                    if (value >= 0xffff || value <= 0) {
                        errors.incrementAndGet();
                    }
                    value = state.decrementAndGet();
                    if (value >= 0xffff || value < 0) {
                        errors.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    return;
                } finally {
                    l.readUnlock();
                }
            }
        };
        for (int i = 0; i < threads; i += 2) {
            spawnThread(readerRunnable);
            spawnThread(writerRunnable);
        }
        start.countDown();
        joinAllThreads();
        assertEquals(0, errors.get());
    }

    private static final long TIMEOUT_MILLIS = 5_000;

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

    private boolean threadIsWaiting(Thread t) {
        Thread.State s = t.getState();
        return s == Thread.State.WAITING || s == Thread.State.TIMED_WAITING;
    }

    private void waitFor(Supplier<Boolean> predicate, String name) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (predicate.get()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
        fail("test timed out while waiting for a predicate [" + name + "] to become true");
    }
}
