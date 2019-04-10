import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class CacheNode {
    public static final int availableProcessors = 4;
    // First node in the cache
    // Holds statistics about the cache
    AtomicReference<GenNode> parent;
    AtomicReferenceArray<Integer> misses;
    CacheNode(GenNode parent, int level) {
        misses = new AtomicReferenceArray<>(availableProcessors * Math.min(16, level));
        this.parent = new AtomicReference<>(parent);
    }
    int getPos() {
        long id = Thread.currentThread().getId();
        int pos = (Math.toIntExact(id ^ (id >>> 16)) & (misses.length() - 1));
        return pos;
    }

    int approximateMissCount() {
        return misses.get(0);
    }

    void resetMissCount() {
        misses.set(0, 0);
    }

    void bumpMissCount() {
        misses.set(0, misses.get(0) + 1);
    }
}
