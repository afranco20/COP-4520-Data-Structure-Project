
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class Cache {
    AtomicReference<CacheNode> stats;
    AtomicReferenceArray<GenNode> root;

    void createCacheLev(int level) {
        // Get rid of "1 +" since stats is not contained in the array
        root = new AtomicReferenceArray<>(1 +(1 << level));
        stats.set(new CacheNode(null, 8));
    }

}
