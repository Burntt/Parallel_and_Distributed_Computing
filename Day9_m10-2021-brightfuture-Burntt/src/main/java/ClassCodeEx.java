import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class ClassCodeEx {
    private static int threadCount = 5;

    public static int mySuperFunc(int value) {
        return value + 1;
    }

    static class MakeDelayFunction implements Function<Long, CompletableFuture<Void>> {
        private ScheduledExecutorService pool;

        public MakeDelayFunction(ScheduledExecutorService pool1) {
            pool = pool1;
        }

        @Override
        public CompletableFuture<Void> apply(Long delayMs) {
            CompletableFuture<Void> result = new CompletableFuture<Void>();
            pool.schedule(() -> {
                result.complete(null);
            }, delayMs, TimeUnit.MILLISECONDS);
            return result;
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(threadCount);
        ScheduledExecutorService pool2 = Executors.newScheduledThreadPool(threadCount);

        Supplier<String> supplier = () -> {
            long id = Thread.currentThread().getId();
            System.out.printf("Thread #%d: %s.%n", id, "Supplier Started");
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(3000));
            } catch (InterruptedException ex) {
            }
            System.out.printf("Thread #%d: %s.%n", id, "Supplier Completed");
            return "Val" + Thread.currentThread().getId();
        };

        Function<Integer, CompletableFuture<Integer>> slowAdd10 = (value) -> {
            long id = Thread.currentThread().getId();
            System.out.printf("Thread #%d: %s.%n", id, "Slowling adding 10 to " + value);
            return CompletableFuture.supplyAsync(() -> {
                long id1 = Thread.currentThread().getId();
                System.out.printf("Thread #%d -> #%d: %s.%n", id, id1,
                        "Sleeping before adding 10 to " + value);
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(3000));
                } catch (InterruptedException e) {
                }
                return value + 10;
            }, pool);
        };

        // This is an expanded version of lambda-function: (delayMs) -> { ... }
        Function<Long, CompletableFuture<Void>> makeDelay = new MakeDelayFunction(pool);

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; ++i) {
            System.out.println("Starting");

            CompletableFuture<String> firstFuture = CompletableFuture.supplyAsync(supplier, pool)
                    .thenApply((s) -> s + "Suffix").thenApply((s) -> "Prefix" + s);

            final long delayMs = ThreadLocalRandom.current().nextLong(1500);
            CompletableFuture<String> secondFuture =
                    makeDelay.apply(delayMs).thenApply((unused) -> "timeout!");

            CompletableFuture.anyOf(firstFuture, secondFuture).thenAccept((o) -> {
                System.out.println("Winner value (alt) is: " + o);
            });

            CompletableFuture<String> applyToEitherFuture =
                    firstFuture.applyToEither(secondFuture, (value) -> {
                        System.out.println("Winner value is: " + value);
                        return value;
                    });

            CompletableFuture<Integer> finalFuture = applyToEitherFuture.thenCompose((value) -> {
                return makeDelay.apply(1000L).thenApply((unused) -> {
                    System.out.println("Winner value is: " + value + " [after extra 1s]");
                    return 17;
                });
            });

            futures.add(finalFuture);
        }

        CompletableFuture<?> cfs[] = new CompletableFuture<?>[futures.size()];
        futures.toArray(cfs);

        CompletableFuture.allOf(cfs).thenRun(() -> {
            System.out.println("all the values are there!");
            for (var future : futures) {
                System.out.println("retval is " + future.join());
            }
        }).join();

        System.out.println("calling shutdown");

        pool.shutdown();
        pool2.shutdown();

        pool.awaitTermination(10, TimeUnit.SECONDS);
        pool2.awaitTermination(10, TimeUnit.SECONDS);

        /*
         * for (int i = 0; i < threadCount; ++i) { System.out.println("Starting");
         * 
         * CompletableFuture<String> initialStr = CompletableFuture.supplyAsync(supplier, pool);
         * 
         * CompletableFuture<String> transformed = initialStr .thenApply((s) -> s + "Suffix")
         * .thenApply((s) -> "Prefix" + s);
         * 
         * CompletableFuture<Void> transformedAndDelayed = transformed.thenCompose((value) -> {
         * final long delayMs = ThreadLocalRandom.current().nextLong(5000); return
         * makeDelay.apply(delayMs); });
         * 
         * transformed.thenAcceptBoth(transformedAndDelayed, (value, unused) -> {
         * System.out.println("Received " + value + " after delay"); }); }
         */

        // for (int i = 0; i < threadCount; ++i) {
        // final long delayMs = ThreadLocalRandom.current().nextLong(5000);
        // makeDelay.apply(delayMs).thenRun(() -> {
        // System.out.println("after delay " + delayMs + "!");
        // });
        // }
    }
}
