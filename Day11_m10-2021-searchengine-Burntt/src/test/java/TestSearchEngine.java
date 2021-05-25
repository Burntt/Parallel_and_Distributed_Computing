import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

public class TestSearchEngine {
    @Test
    public void testCorrectness() {
        final int shardCount = 3;
        final int replicaCount = 1;

        ConcurrentSkipListSet<Integer> touchedShards = new ConcurrentSkipListSet<>();
        HashSet<Integer> allShards = new HashSet<>();
        for (int index = 0; index < shardCount; ++index) {
            allShards.add(index);
        }

        SearchContext ctx = new SearchContext() {
            @Override
            public CompletableFuture<List<DocumentWithRelevance>> searchDownstream(int shard,
                    int replica, Object query) {
                List<DocumentWithRelevance> result = new ArrayList<>(10);
                for (int i = 0; i < 3; ++i) {
                    result.add(new DocumentWithRelevance(String.format("common-doc-%d", i),
                            100 + ThreadLocalRandom.current().nextInt(100)));
                }
                for (int i = 0; i < 7; ++i) {
                    result.add(new DocumentWithRelevance(
                            String.format("shard-%d-replica-%d-doc-%d", shard, replica, i),
                            ThreadLocalRandom.current().nextInt(100)));
                }
                touchedShards.add(shard);
                return CompletableFuture.completedFuture(result);
            }

            @Override
            public int getShardCount() {
                return shardCount;
            }

            @Override
            public int getReplicaCount(int shard) {
                return replicaCount;
            }
        };

        SearchEngine engine = ReplacesUtils.instance(SearchEngine.class, ctx);

        List<DocumentWithRelevance> result1 = engine.search(new Object(), 1).join();
        assertEquals(1, result1.size());
        assertEquals(0, result1.get(0).getDocumentId().indexOf("common-doc-"));
        assertEquals(allShards, touchedShards);

        List<DocumentWithRelevance> result2 = engine.search(new Object(), 5).join();
        assertEquals(5, result2.size(), "limit is respected");
        assertEquals(5,
                result2.stream().map(DocumentWithRelevance::getDocumentId).distinct().count(),
                "all the results must be unique");
        assertEquals(allShards, touchedShards);

        engine.shutdownAndAwaitTermination();
    }
}
