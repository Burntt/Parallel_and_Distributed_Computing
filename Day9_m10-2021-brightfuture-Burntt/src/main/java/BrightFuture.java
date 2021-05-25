import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.allOf;

public class BrightFuture {
    // Concatenate strings `left` and `right` with the separator `separator`.
    // Convert the result to the uppercase.

    public CompletableFuture<String> concatAndUp(CompletableFuture<String> left, CompletableFuture<String> right,
                                                 String separator) {
        return allOf(left, right)
                .thenApply(v -> left
                        .join()
                        .toUpperCase() + separator + right
                        .join()
                        .toUpperCase());
    }

    // Concatenate all the parts together with the separator.
    // Exactly in the same order as they are provided.
    public CompletableFuture<String> concatMany(List<CompletableFuture<String>> parts, String separator) {
        CompletableFuture<Void> allFutures = allOf(parts.toArray(new CompletableFuture<?>[0]));
        return allFutures.thenApply(future -> {
            return parts.stream().map(CompletableFuture::join).collect(Collectors.joining(separator));
        });
    }

    interface FacebookService {
        CompletableFuture<List<Long>> getLastPostIdForUser(String user);
        CompletableFuture<String> getPostTitleById(long id);
    }

    // Return all the post titles for the given user.
    // Each post comes on a new line.
    public CompletableFuture<String> renderFacebookProfilePage(FacebookService service, String user) {
        return service.getLastPostIdForUser(user).handleAsync((lastPostIdForUser, e) -> {
            if (e != null) {
                return null;
            }
            return concatMany(lastPostIdForUser
                            .stream()
                            .map(service::getPostTitleById).collect(Collectors.<CompletableFuture<String>>toList()),
                    "\n")
                    .join();
        });
    }

    interface GoogleService {
        CompletableFuture<Set<String>> searchDocuments(String query);
    }

    // Return all the documents that match all of the three provided queries.
    public CompletableFuture<Set<String>> retrieveMatchingDocuments(GoogleService service,
            String firstQuery, String secondQuery, String thirdQuery) {
        List<CompletableFuture<Set<String>>> listOfFutureQueries = asList(service.searchDocuments(firstQuery),
                service.searchDocuments(secondQuery), service.searchDocuments(thirdQuery));
        CompletableFuture<Void> documents = allOf(listOfFutureQueries.toArray(new CompletableFuture<?>[0]));
        return documents.thenApply(new Function<Void, Set<String>>() {
            private void accept(HashSet<String> output1, Set<String> document) {
                if (output1.size() != 0) {
                    output1.retainAll(document);
                } else {
                    output1.addAll(document);
                }
            }

            @Override
            public Set<String> apply(Void future) {
                return listOfFutureQueries.stream().map(CompletableFuture::join)
                        .collect(Collector.of(HashSet::new, this::accept, (output1, output2) -> {
                            if (output1.size() != 0) {
                                output1.retainAll(output2);
                                return output1;
                            } else {
                                return output2;
                            }
                        }));
            }
        });
    }

    // Try to call the value provider with the given key.
    // If the computation fails, retry it.
    // Keep trying up to `limit` attempts and then fail the resulting future.
    public CompletableFuture<String> retryUntilSuccess(
            Function<Long, CompletableFuture<String>> valueProvider, Long key, int limit) {

        CompletableFuture<String> futureResult = new CompletableFuture<String>();
        CompletableFuture.runAsync(() -> {

            for (int i = 0; i < limit; i++) {
                try {
                    valueProvider.apply(key).thenAcceptAsync(futureResult::complete).get();
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println("Execution exception" + e);
                }
                if (futureResult.isDone()) break;
            }

            if (!futureResult.isDone()) {
                futureResult.completeExceptionally(new Exception("too many retries"));
            }

        });

        return futureResult;
    }

    // Try to poll the result with the given key.
    // If the result is not ready yet or the poll failed, retry.
    // Keep trying up to `limit` attempts and then fail the resulting future.
    public CompletableFuture<Void> pollUntilCompletion(
            Supplier<CompletableFuture<Boolean>> completionProvider, int limit) {

            CompletableFuture<Void> futureResult = new CompletableFuture<Void>();
            CompletableFuture.runAsync(() -> {

                for (int i = 0; i < limit; i++) {
                    try {
                        completionProvider.get().thenAccept(s -> { if (s) futureResult.complete(null); }).get();
                    } catch (InterruptedException | ExecutionException e) {
                        System.out.println("Execution exception" + e);
                }
                    if (futureResult.isDone()) {
                        break;
                    }
                }

                if (!futureResult.isDone()) {
                    futureResult.completeExceptionally(new Exception("too many retries"));
                }

            });

            return futureResult;
    }

    // Return the future to the index of the first computed value.
    // If all the futures fail, fail the resulting future.
    public CompletableFuture<Integer> pollFirstFuture(List<CompletableFuture<String>> futures) {
        return CompletableFuture.anyOf(futures.toArray(new CompletableFuture<?>[0]))
                .thenApply(future -> {
                    for (CompletableFuture<String> p : futures) {
                        if (p.isDone() && !p.isCancelled() && !p.isCompletedExceptionally()) {
                            Integer indexOf = futures.indexOf(p);
                            return Optional.of(indexOf).get();
                        }
                    }

                    if (Optional.<Integer>empty().get() != null) {
                        return Optional.<Integer>empty().get();
                    }
                    return null;
                });
    }

    // Retrieve the value for the given key by using the primary provider.
    // If the primary provider fails, fallback to the backup provider.
    // If the backup provider fails, return the default value.
    public CompletableFuture<String> primaryOrBackupOrDefault(
        Function<Long, CompletableFuture<String>> primaryProvider,
        Function<Long, CompletableFuture<String>> backupProvider, Long key,
        String defaultValue) {
        return primaryProvider.apply(key).handleAsync(new BiFunction<>() {
            @Override
            public String apply(String value, Throwable ext1) {
                if (ext1 != null) {
                    return backupProvider.apply(key).handleAsync(new BiFunction<String, Throwable, String>() {
                        @Override
                        public String apply(String value2, Throwable ext2) {
                            if (ext2 != null) {
                                return defaultValue;
                            }
                            return value2;
                        }
                    }).join();
                }
                return value;
            }
        });
    }

    // Returns the quorum decision on a particular vote.
    // If there are at least two YES-votes, then the outcome is immediately YES.
    // If there are at least two NO-votes, then the outcome is immediately NO.
    // Failed votes are treated as NO-votes.
    public CompletableFuture<Boolean> quorumVote(CompletableFuture<Boolean> firstVote,
            CompletableFuture<Boolean> secondVote, CompletableFuture<Boolean> thirdVote) {

        int THRESHOLD = 3;
        CompletableFuture<Boolean> voteOutcome = new CompletableFuture<Boolean>();
        final AtomicInteger terminatedVote = new AtomicInteger();
        final AtomicInteger voteResult = new AtomicInteger();
        BiConsumer<? super Boolean, ? super Throwable> BiHandler = new BiConsumer<>() {
            @Override
            public void accept(Boolean response, Throwable ext) {
                int count = terminatedVote.incrementAndGet();
                if (ext == null && response) {
                    if (THRESHOLD - 1 <= voteResult.incrementAndGet() || THRESHOLD <= count) {
                        voteOutcome.complete(THRESHOLD - 1 <= voteResult.get());
                    }
                } else {
                    if (((count == 0 || voteResult.get() != 0) && (count < THRESHOLD || voteResult.get() == THRESHOLD - 1))) {
                        return;
                    }
                    voteOutcome.complete(false);
                }
            }
        };
        firstVote.whenCompleteAsync(BiHandler);
        secondVote.whenCompleteAsync(BiHandler);
        thirdVote.whenCompleteAsync(BiHandler);
        return voteOutcome;
    }
}
