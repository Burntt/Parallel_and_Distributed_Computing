import static org.junit.jupiter.api.Assertions.fail;
import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import com.google.common.base.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class TestEastWestBridgeProblem {
    private final boolean verbose = false;

    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    @ParameterizedTest(name = "testOneCarPasses [{index}] {argumentsWithNames}")
    @EnumSource(Direction.class)
    void testOneCarPasses(Direction direction) throws InterruptedException {
        BridgeControl control = ReplacesUtils.instance(BridgeControlImpl.class);
        control.onArrive(direction);
        control.onDepart();
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    void testBlocksOnBridgeCapacity(Direction direction) throws InterruptedException {
        BridgeControl control = ReplacesUtils.instance(BridgeControlImpl.class);
        for (int index = 0; index < EastWestBridgeProblem.BRIDGE_CAPACITY; ++index) {
            Logging.info("onArrive(" + direction.toString() + ")");
            control.onArrive(direction);
        }
        AtomicLong step = new AtomicLong();
        step.set(0);
        Thread t = spawnThread(() -> {
            step.set(1);
            try {
                Logging.info("onArrive(" + direction.toString() + ") extra begin");
                control.onArrive(direction);
                Logging.info("onArrive(" + direction.toString() + ") extra end");
            } catch (InterruptedException ignored) {
            } finally {
                step.set(2);
            }
        });
        waitFor(() -> step.get() == 1 && threadIsWaiting(t), "extra car is waiting");
        control.onDepart();
        waitFor(() -> step.get() == 2, "extra car has entered the bridge");
        joinAllThreads();
    }

    @Test
    void testEastBlocksWest() throws InterruptedException {
        BridgeControl control = ReplacesUtils.instance(BridgeControlImpl.class);
        control.onArrive(Direction.EAST);
        AtomicLong step = new AtomicLong();
        step.set(0);
        Thread t = spawnThread(() -> {
            step.set(1);
            try {
                control.onArrive(Direction.WEST);
            } catch (InterruptedException e) {
            } finally {
                step.set(2);
            }
        });
        waitFor(() -> step.get() == 1 && threadIsWaiting(t), "extra car is waiting");
        control.onDepart();
        waitFor(() -> step.get() == 2, "extra car has entered the bridge");
        joinAllThreads();
    }

    @Test
    void testWestBlocksEast() throws InterruptedException {
        BridgeControl control = ReplacesUtils.instance(BridgeControlImpl.class);
        control.onArrive(Direction.WEST);
        AtomicLong step = new AtomicLong();
        step.set(0);
        Thread t = spawnThread(() -> {
            step.set(1);
            try {
                control.onArrive(Direction.EAST);
            } catch (InterruptedException e) {
            } finally {
                step.set(2);
            }
        });
        waitFor(() -> step.get() == 1 && threadIsWaiting(t), "extra car is waiting");
        control.onDepart();
        waitFor(() -> step.get() == 2, "extra car has entered the bridge");
        joinAllThreads();
    }

    @ParameterizedTest(name = "testOneDirectionOnly [{index}] {argumentsWithNames}")
    @EnumSource(Direction.class)
    void testOneDirectionOnly(Direction direction) throws InterruptedException {
        BridgeControl control = ReplacesUtils.instance(BridgeControlImpl.class);
        SafetyAndSaturationControl safetyAndSaturationControl =
                new SafetyAndSaturationControl(control);
        EastWestBridgeProblem.simulate(safetyAndSaturationControl, 100, Duration.ofSeconds(3), 10.0,
                () -> direction, verbose);
        safetyAndSaturationControl.checkSafety();
        safetyAndSaturationControl.checkSaturation();
    }

    @Test
    void testLowLoad() throws InterruptedException {
        BridgeControl control = ReplacesUtils.instance(BridgeControlImpl.class);
        SafetyAndSaturationControl safetyAndSaturationControl =
                new SafetyAndSaturationControl(control);
        EastWestBridgeProblem.simulate(safetyAndSaturationControl, 100, Duration.ofSeconds(10), 0.2,
                EastWestBridgeProblem.RANDOM_DIRECTION_SUPPLIER, verbose);
        safetyAndSaturationControl.checkSafety();
    }

    @Test
    void testMediumLoad() throws InterruptedException {
        BridgeControl control = ReplacesUtils.instance(BridgeControlImpl.class);
        SafetyAndSaturationControl safetyAndSaturationControl =
                new SafetyAndSaturationControl(control);
        EastWestBridgeProblem.simulate(safetyAndSaturationControl, 100, Duration.ofSeconds(10), 1.0,
                EastWestBridgeProblem.RANDOM_DIRECTION_SUPPLIER, verbose);
        safetyAndSaturationControl.checkSafety();
    }

    @Test
    void testHighLoad() throws InterruptedException {
        BridgeControl control = ReplacesUtils.instance(BridgeControlImpl.class);
        SafetyAndSaturationControl safetyAndSaturationControl =
                new SafetyAndSaturationControl(control);
        EastWestBridgeProblem.simulate(safetyAndSaturationControl, 100, Duration.ofSeconds(10), 5.0,
                EastWestBridgeProblem.RANDOM_DIRECTION_SUPPLIER, verbose);
        safetyAndSaturationControl.checkSafety();
    }

    static class SafetyAndSaturationControl implements BridgeControl {
        private final BridgeControl delegate;
        private boolean safeCount = true;
        private String unsafeCountExample = null;
        private boolean safeDirection = true;
        private String unsafeDirectionExample = null;
        private int currentCount = 0;
        private Direction currentDirection = null;
        private final AtomicIntegerArray distribution =
                new AtomicIntegerArray(EastWestBridgeProblem.BRIDGE_CAPACITY);
        private final Set<String> onBridge = new HashSet<>();

        SafetyAndSaturationControl(BridgeControl delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onArrive(Direction direction) throws InterruptedException {
            delegate.onArrive(direction);
            synchronized (this) {
                onBridge.add(Thread.currentThread().getName());
                ++currentCount;
                if (currentCount > EastWestBridgeProblem.BRIDGE_CAPACITY) {
                    safeCount = false;
                    if (unsafeCountExample == null) {
                        unsafeCountExample = onBridge.toString();
                    }
                } else {
                    distribution.getAndIncrement(currentCount - 1);
                }
                if (currentDirection == null) {
                    currentDirection = direction;
                }
                if (currentDirection != direction) {
                    safeDirection = false;
                    unsafeDirectionExample = onBridge.toString();
                }
            }
        }

        @Override
        public void onDepart() {
            synchronized (this) {
                --currentCount;
                if (currentCount == 0) {
                    currentDirection = null;
                }
            }
            delegate.onDepart();
        }

        synchronized void checkSafety() {
            if (!safeCount) {
                fail("bridge must not be overloaded; counter-example: " + unsafeCountExample
                        + " were on the bridge");
            }
            if (!safeDirection) {
                fail("bridge must not be travelled in opposite directions; counter-example: "
                        + unsafeDirectionExample + " were on the bridge");
            }
        }

        synchronized void checkSaturation() {
            int saturated = 0;
            int unsaturated = 0;
            for (int index = 0; index < distribution.length(); ++index) {
                if (index + 1 == distribution.length()) {
                    saturated += distribution.get(index);
                } else {
                    unsaturated += distribution.get(index);
                }
            }
            double saturation = (double) saturated / (double) (saturated + unsaturated);
            if (saturation < 0.9) {
                fail("bridge must be used efficiently; actual saturation: " + saturation
                        + ", expected: >0.9");
            }
        }
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
