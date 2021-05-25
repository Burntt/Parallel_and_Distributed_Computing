import java.util.List;
import java.util.concurrent.CompletableFuture;

////////////////////////////////////////////////////////////////////////////////
// TODO: This is the interface you should use.
////////////////////////////////////////////////////////////////////////////////
interface SearchContext {
    // Returns total number of shards.
    int getShardCount();

    // Returns total number of replicas of a given shard.
    int getReplicaCount(int shard);

    // This is a black-box implementation of the backend service.
    // No guarantees on document ids returned from different shards.
    CompletableFuture<List<DocumentWithRelevance>> searchDownstream(int shard, int replica, Object query);
}
