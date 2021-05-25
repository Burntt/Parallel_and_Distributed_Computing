import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestBrightFuture {
    private static long CALL_TIMEOUT_NS = 50_000_000L;
    private static long JOIN_TIMEOUT_NS = 15_000_000_000L;

    private BrightFuture impl = ReplacesUtils.instance(BrightFuture.class);
    private ScheduledExecutorService executor;

    @BeforeEach
    void before() {
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterEach
    void after() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(15, TimeUnit.SECONDS);
        executor = null;
    }

    @Test
    public void testConcatAndUp() throws ExecutionException, InterruptedException {
        long beforeCallNs = System.nanoTime();
        CompletableFuture<String> future =
                impl.concatAndUp(delayed("hello", 1000, TimeUnit.MILLISECONDS),
                        delayed("world", 1500, TimeUnit.MILLISECONDS), ", ");
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        String result = future.join();
        long afterJoinNs = System.nanoTime();

        assertEquals("HELLO, WORLD", result);
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testConcatMany() {
        List<CompletableFuture<String>> parts = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            parts.add(delayed("+" + Integer.toString(index), index * 100, TimeUnit.MILLISECONDS));
        }
        for (int index = 0; index < 10; index++) {
            parts.add(delayed("-" + Integer.toString(index), (10 - index) * 100,
                    TimeUnit.MILLISECONDS));
        }
        parts.add(delayed(".", 0, TimeUnit.MILLISECONDS));
        long beforeCallNs = System.nanoTime();
        CompletableFuture<String> future = impl.concatMany(parts, " ");
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        String result = future.join();
        long afterJoinNs = System.nanoTime();

        assertEquals("+0 +1 +2 +3 +4 +5 +6 +7 +8 +9 -0 -1 -2 -3 -4 -5 -6 -7 -8 -9 .", result);
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testRenderFacebookProfilePage() {
        BrightFuture.FacebookService svc = new BrightFuture.FacebookService() {
            @Override
            public CompletableFuture<List<Long>> getLastPostIdForUser(String user) {
                long delay = nearlySecondMillis();
                return delayed(() -> generatePostIds(user), delay, TimeUnit.MILLISECONDS);
            }

            @Override
            public CompletableFuture<String> getPostTitleById(long id) {
                long delay = nearlySecondMillis();
                return delayed(() -> generatePostTitle(id), delay, TimeUnit.MILLISECONDS);
            }

            private List<Long> generatePostIds(String user) {
                List<Long> values = new ArrayList<>();
                long hash = user.chars().reduce((a, b) -> 31 * a + b).getAsInt();
                long currentId = 10000 + hash % 10000;
                for (int index = 0; index < 10; ++index) {
                    values.add(currentId);
                    hash *= 17;
                    currentId += hash % 31;
                }
                return Collections.unmodifiableList(values);
            }

            private String generatePostTitle(long id) {
                return "#" + Long.toString(id) + ": Scandal post!";
            }
        };

        long beforeCallNs = System.nanoTime();
        CompletableFuture<String> future = impl.renderFacebookProfilePage(svc, "root");
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        String result = future.join();
        long afterJoinNs = System.nanoTime();

        assertEquals(
                "#16402: Scandal post!\n#16421: Scandal post!\n#16434: Scandal post!\n#16438: Scandal post!\n#16444: Scandal post!\n#16453: Scandal post!\n#16482: Scandal post!\n#16510: Scandal post!\n#16521: Scandal post!\n#16522: Scandal post!",
                result);
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testRetrieveMatchingDocuments() {
        BrightFuture.GoogleService svc = new BrightFuture.GoogleService() {
            @Override
            public CompletableFuture<Set<String>> searchDocuments(String query) {
                long delay = nearlySecondMillis();
                if (query.equals("first")) {
                    return delayed(() -> setOf("foo", "bar", "baz"), delay, TimeUnit.MILLISECONDS);
                } else if (query.equals("second")) {
                    return delayed(() -> setOf("spam", "ham", "baz"), delay, TimeUnit.MILLISECONDS);
                } else if (query.equals("third")) {
                    return delayed(() -> setOf("spam", "bar", "baz"), delay, TimeUnit.MILLISECONDS);
                } else {
                    return delayed(() -> setOf(), delay, TimeUnit.MILLISECONDS);
                }
            }

        };

        long beforeCallNs = System.nanoTime();
        CompletableFuture<Set<String>> future =
                impl.retrieveMatchingDocuments(svc, "first", "second", "third");
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        Set<String> result = future.join();
        long afterJoinNs = System.nanoTime();

        assertEquals(setOf("baz"), result);
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    private Function<Long, CompletableFuture<String>> getLongToStringValueProvider(int failures) {
        return getLongToStringValueProvider(failures, "");
    }

    private Function<Long, CompletableFuture<String>> getLongToStringValueProvider(int failures,
            String prefix) {
        return new Function<Long, CompletableFuture<String>>() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public CompletableFuture<String> apply(Long t) {
                if (counter.incrementAndGet() < failures) {
                    return delayedFailure(prefix + "Whoops!", 1000, TimeUnit.MILLISECONDS);
                } else {
                    return delayed(prefix + Long.toHexString(t).toUpperCase(), 1000,
                            TimeUnit.MILLISECONDS);
                }
            }
        };
    }

    @Test
    public void testRetryUntilSuccess_NoFailures() {
        Function<Long, CompletableFuture<String>> valueProvider = getLongToStringValueProvider(0);

        long beforeCallNs = System.nanoTime();
        CompletableFuture<String> future = impl.retryUntilSuccess(valueProvider, 0xdeadbeefl, 3);
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        String result = future.join();
        long afterJoinNs = System.nanoTime();

        assertEquals("DEADBEEF", result);
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testRetryUntilSuccess_TwoFailures() {
        Function<Long, CompletableFuture<String>> valueProvider = getLongToStringValueProvider(2);

        long beforeCallNs = System.nanoTime();
        CompletableFuture<String> future = impl.retryUntilSuccess(valueProvider, 0xdeadbeefl, 3);
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        String result = future.join();
        long afterJoinNs = System.nanoTime();

        assertEquals("DEADBEEF", result);
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testRetryUntilSuccess_ThreeFailures() {
        Function<Long, CompletableFuture<String>> valueProvider = getLongToStringValueProvider(3);

        long beforeCallNs = System.nanoTime();
        CompletableFuture<String> future = impl.retryUntilSuccess(valueProvider, 0xdeadbeefl, 3);
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        String result = future.join();
        long afterJoinNs = System.nanoTime();

        assertEquals("DEADBEEF", result);
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testRetryUntilSuccess_TenFailures() {
        Function<Long, CompletableFuture<String>> valueProvider = getLongToStringValueProvider(10);

        long beforeCallNs = System.nanoTime();
        CompletableFuture<String> future = impl.retryUntilSuccess(valueProvider, 0xdeadbeefl, 3);
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = 0;
        long afterJoinNs = 0;
        String result = null;
        boolean exceptionThrown = false;
        try {
            beforeJoinNs = System.nanoTime();
            result = future.join();
            fail("expected an exception to be thrown");
        } catch (CompletionException e) {
            result = null;
            exceptionThrown = true;
        } finally {
            afterJoinNs = System.nanoTime();
        }

        assertNull(result);
        assertTrue(exceptionThrown);
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    private Supplier<CompletableFuture<Boolean>> getBooleanProvider(int negatives) {
        return new Supplier<CompletableFuture<Boolean>>() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public CompletableFuture<Boolean> get() {
                if (counter.incrementAndGet() < negatives) {
                    return delayed(false, 1000, TimeUnit.MILLISECONDS);
                } else {
                    return delayed(true, 1000, TimeUnit.MILLISECONDS);
                }
            }
        };
    }

    @Test
    public void testPollUntilCompletion_NoFailures() {
        Supplier<CompletableFuture<Boolean>> completionProvider = getBooleanProvider(0);

        long beforeCallNs = System.nanoTime();
        CompletableFuture<Void> future = impl.pollUntilCompletion(completionProvider, 3);
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        future.join();
        long afterJoinNs = System.nanoTime();

        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testPollUntilCompletion_TwoFailures() {
        Supplier<CompletableFuture<Boolean>> completionProvider = getBooleanProvider(2);

        long beforeCallNs = System.nanoTime();
        CompletableFuture<Void> future = impl.pollUntilCompletion(completionProvider, 3);
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        future.join();
        long afterJoinNs = System.nanoTime();

        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testPollUntilCompletion_ThreeFailures() {
        Supplier<CompletableFuture<Boolean>> completionProvider = getBooleanProvider(3);

        long beforeCallNs = System.nanoTime();
        CompletableFuture<Void> future = impl.pollUntilCompletion(completionProvider, 3);
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        future.join();
        long afterJoinNs = System.nanoTime();

        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testPollUntilCompletion_TenFailures() {
        Supplier<CompletableFuture<Boolean>> completionProvider = getBooleanProvider(10);

        long beforeCallNs = System.nanoTime();
        CompletableFuture<Void> future = impl.pollUntilCompletion(completionProvider, 3);
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = 0;
        long afterJoinNs = 0;
        boolean exceptionThrown = false;
        try {
            beforeJoinNs = System.nanoTime();
            future.join();
            fail("expected an exception to be thrown");
        } catch (CompletionException e) {
            exceptionThrown = true;
        } finally {
            afterJoinNs = System.nanoTime();
        }

        assertTrue(exceptionThrown);
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testPollFirstFuture() {
        List<CompletableFuture<String>> pollables = new ArrayList<>();
        pollables.add(delayed("A", 500, TimeUnit.MILLISECONDS));
        pollables.add(delayed("B", 600, TimeUnit.MILLISECONDS));
        pollables.add(delayed("C", 700, TimeUnit.MILLISECONDS));
        pollables.add(delayed("D", 800, TimeUnit.MILLISECONDS));
        pollables.add(delayed("E", 900, TimeUnit.MILLISECONDS));
        pollables.add(delayed("F", 1000, TimeUnit.MILLISECONDS));
        pollables.add(delayed("G", 1100, TimeUnit.MILLISECONDS));
        pollables.add(delayed("H", 1200, TimeUnit.MILLISECONDS));
        pollables.add(delayed("I", 2000, TimeUnit.MILLISECONDS));
        pollables.add(delayed("J", 3000, TimeUnit.MILLISECONDS));
        pollables.add(delayed("K", 10000, TimeUnit.MILLISECONDS));
        Collections.shuffle(pollables);

        long beforeCallNs = System.nanoTime();
        CompletableFuture<Integer> future = impl.pollFirstFuture(pollables);
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        int readyIndex = future.join();
        long afterJoinNs = System.nanoTime();

        assertTrue(pollables.get(readyIndex).isDone());
        assertEquals("A", pollables.get(readyIndex).join());

        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testPrimaryOrBackupOrDefault_Primary() {
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger backupCalls = new AtomicInteger();
        Function<Long, CompletableFuture<String>> primaryProvider =
                new Function<Long, CompletableFuture<String>>() {
                    @Override
                    public CompletableFuture<String> apply(Long t) {
                        primaryCalls.incrementAndGet();
                        return delayed("PRIMARY:" + Long.toString(t), 1000, TimeUnit.MILLISECONDS);
                    }
                };
        Function<Long, CompletableFuture<String>> backupProvider =
                new Function<Long, CompletableFuture<String>>() {
                    @Override
                    public CompletableFuture<String> apply(Long t) {
                        backupCalls.incrementAndGet();
                        return delayed("BACKUP:" + Long.toString(t), 1000, TimeUnit.MILLISECONDS);
                    }
                };

        long beforeCallNs = System.nanoTime();
        CompletableFuture<String> future =
                impl.primaryOrBackupOrDefault(primaryProvider, backupProvider, 42L, "DEFAULT");
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        String result = future.join();
        long afterJoinNs = System.nanoTime();

        assertEquals("PRIMARY:42", result);
        assertEquals(1, primaryCalls.get());
        assertEquals(0, backupCalls.get());
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testPrimaryOrBackupOrDefault_Backup() {
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger backupCalls = new AtomicInteger();
        Function<Long, CompletableFuture<String>> primaryProvider =
                new Function<Long, CompletableFuture<String>>() {
                    @Override
                    public CompletableFuture<String> apply(Long t) {
                        primaryCalls.incrementAndGet();
                        return delayedFailure("PRIMARY:" + Long.toString(t), 1000,
                                TimeUnit.MILLISECONDS);
                    }
                };
        Function<Long, CompletableFuture<String>> backupProvider =
                new Function<Long, CompletableFuture<String>>() {
                    @Override
                    public CompletableFuture<String> apply(Long t) {
                        backupCalls.incrementAndGet();
                        return delayed("BACKUP:" + Long.toString(t), 1000, TimeUnit.MILLISECONDS);
                    }
                };

        long beforeCallNs = System.nanoTime();
        CompletableFuture<String> future =
                impl.primaryOrBackupOrDefault(primaryProvider, backupProvider, 42L, "DEFAULT");
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        String result = future.join();
        long afterJoinNs = System.nanoTime();

        assertEquals("BACKUP:42", result);
        assertEquals(1, primaryCalls.get());
        assertEquals(1, backupCalls.get());
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testPrimaryOrBackupOrDefault_Default() {
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger backupCalls = new AtomicInteger();
        Function<Long, CompletableFuture<String>> primaryProvider =
                new Function<Long, CompletableFuture<String>>() {
                    @Override
                    public CompletableFuture<String> apply(Long t) {
                        primaryCalls.incrementAndGet();
                        return delayedFailure("PRIMARY:" + Long.toString(t), 1000,
                                TimeUnit.MILLISECONDS);
                    }
                };
        Function<Long, CompletableFuture<String>> backupProvider =
                new Function<Long, CompletableFuture<String>>() {
                    @Override
                    public CompletableFuture<String> apply(Long t) {
                        backupCalls.incrementAndGet();
                        return delayedFailure("BACKUP:" + Long.toString(t), 1000,
                                TimeUnit.MILLISECONDS);
                    }
                };

        long beforeCallNs = System.nanoTime();
        CompletableFuture<String> future =
                impl.primaryOrBackupOrDefault(primaryProvider, backupProvider, 42L, "DEFAULT");
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        String result = future.join();
        long afterJoinNs = System.nanoTime();

        assertEquals("DEFAULT", result);
        assertEquals(1, primaryCalls.get());
        assertEquals(1, backupCalls.get());
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testQuorumVote_FastYes() {
        long beforeCallNs = System.nanoTime();
        CompletableFuture<Boolean> future =
                impl.quorumVote(delayed(true, 1000, TimeUnit.MILLISECONDS),
                        delayed(true, 1000, TimeUnit.MILLISECONDS),
                        delayed(true, 10000, TimeUnit.MILLISECONDS));
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        boolean result = future.join();
        long afterJoinNs = System.nanoTime();

        assertEquals(true, result);
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testQuorumVote_FastNo() {
        long beforeCallNs = System.nanoTime();
        CompletableFuture<Boolean> future =
                impl.quorumVote(delayed(false, 1000, TimeUnit.MILLISECONDS),
                        delayed(false, 1000, TimeUnit.MILLISECONDS),
                        delayed(false, 10000, TimeUnit.MILLISECONDS));
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        boolean result = future.join();
        long afterJoinNs = System.nanoTime();

        assertEquals(false, result);
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testQuorumVote_FailureIsNo() {
        long beforeCallNs = System.nanoTime();
        CompletableFuture<Boolean> future =
                impl.quorumVote(delayedFailure("oops", 2000, TimeUnit.MILLISECONDS),
                        delayedFailure("oops", 2000, TimeUnit.MILLISECONDS),
                        delayed(true, 1000, TimeUnit.MILLISECONDS));
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        boolean result = future.join();
        long afterJoinNs = System.nanoTime();

        assertEquals(false, result);
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    @Test
    public void testQuorumVote_WaitForDecision() {
        long beforeCallNs = System.nanoTime();
        CompletableFuture<Boolean> future =
                impl.quorumVote(delayedFailure("oops", 2000, TimeUnit.MILLISECONDS),
                        delayed(true, 300, TimeUnit.MILLISECONDS),
                        delayed(true, 1000, TimeUnit.MILLISECONDS));
        long afterCallNs = System.nanoTime();

        long beforeJoinNs = System.nanoTime();
        boolean result = future.join();
        long afterJoinNs = System.nanoTime();

        assertEquals(true, result);
        assertTrue(afterCallNs - beforeCallNs < CALL_TIMEOUT_NS,
                "impl should not block for more than 50ms");
        assertTrue(afterJoinNs - beforeJoinNs < JOIN_TIMEOUT_NS,
                "result should be provided in no more than 15s");
    }

    static Set<String> setOf(String... strings) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i < strings.length; ++i) {
            result.add(strings[i]);
        }
        return Collections.unmodifiableSet(result);
    }

    static long nearlySecondMillis() {
        long delay = Math.round(1000.0 + 75.0 * ThreadLocalRandom.current().nextGaussian());
        return delay;
    }

    <T> CompletableFuture<T> delayed(T value, long delay, TimeUnit unit) {
        CompletableFuture<T> result = new CompletableFuture<>();
        executor.schedule(() -> result.complete(value), delay, unit);
        return result;
    }

    <T> CompletableFuture<T> delayed(Supplier<T> valueSupplier, long delay, TimeUnit unit) {
        CompletableFuture<T> result = new CompletableFuture<>();
        executor.schedule(() -> {
            try {
                result.complete(valueSupplier.get());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        }, delay, unit);
        return result;
    }

    <T> CompletableFuture<T> delayedFailure(String message, long delay, TimeUnit unit) {
        CompletableFuture<T> result = new CompletableFuture<>();
        executor.schedule(() -> result.completeExceptionally(new RuntimeException(message)), delay,
                unit);
        return result;
    }
}
