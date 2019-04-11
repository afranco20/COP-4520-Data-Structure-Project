
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class Cache {
    AtomicReference<CacheNode> stats;
    AtomicReferenceArray<GenNode> root;

    Cache(int level, Cache parent) {

        root = new AtomicReferenceArray<>(1 +(1 << level));
        stats =  new AtomicReference<>(new CacheNode(parent, 8));
    }

}
