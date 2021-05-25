import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;


class SearchEngine {
    protected final SearchContext ctx;
    private final ExecutorService multiThreadExecutor = Executors.newFixedThreadPool(16);


    public SearchEngine(SearchContext ctx) {
        this.ctx = ctx;
    }

    public static <T> Predicate<T> findByKey(Function<? super T, ?> keyExtractor) {
        ConcurrentHashMap<Object, Boolean> checkedCutKeys = new ConcurrentHashMap<>();
        return t -> null == checkedCutKeys.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE);
    }

    public void shutdownAndAwaitTermination() {
        multiThreadExecutor.shutdown();
        while (!multiThreadExecutor.isShutdown()) {
            Thread.yield();
        }
    }

    public CompletableFuture<List<DocumentWithRelevance>> search(Object query, int limit) {

        int replicaCount;
        int shardCount = ctx.getShardCount();

        List<CompletableFuture<Object>> shareRequests = new ArrayList<CompletableFuture<Object>>();

        for (int i = 0; i < shardCount; i++) {
            replicaCount = ctx.getReplicaCount(i);
            List<CompletableFuture<List<DocumentWithRelevance>>> replicaRequests =
                    new ArrayList<>();
            for (int j = 0; j < replicaCount; ++j) replicaRequests.add(ctx.searchDownstream(i, j, query));
            CompletableFuture<Object> newFeature =
                    CompletableFuture.anyOf(replicaRequests.toArray(new CompletableFuture<?>[0]));
            shareRequests.add(newFeature);

        }

        CompletableFuture<List<DocumentWithRelevance>> searchOutput = CompletableFuture.allOf(shareRequests.toArray(new CompletableFuture<?>[0])).
                handleAsync((res, ex) -> {
                    List<DocumentWithRelevance> toSort = new ArrayList<>();
                    Predicate<DocumentWithRelevance> predicate = findByKey(DocumentWithRelevance::getDocumentId);
                    for (CompletableFuture<Object> r : shareRequests) {
                        List<DocumentWithRelevance> join = (List<DocumentWithRelevance>) r.join();
                        for (DocumentWithRelevance documentWithRelevance : join) {
                            if (predicate.test(documentWithRelevance)) {
                                toSort.add(documentWithRelevance);
                            }
                        }
                    }
                    toSort.sort(Comparator.comparingInt(DocumentWithRelevance::getRelevance).reversed());
                    List<DocumentWithRelevance> list = new ArrayList<>();
                    long limitOuter = limit;
                    for (DocumentWithRelevance documentWithRelevance : toSort) {
                        if (limitOuter-- == 0) break;
                        list.add(documentWithRelevance);
                    }
                    return list;
                });

        System.out.println(searchOutput);

        return searchOutput;
    }
}
