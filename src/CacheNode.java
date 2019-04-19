import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import org.deuce.Atomic;

public class CacheNode {
  private static final int availableProcessors = Runtime.getRuntime().availableProcessors();
  public static final int MAX_NUM_LEVELS = 64;
  // First node in the cache
  // Holds statistics about the cache
  AtomicInteger level;
  AtomicIntegerArray sample;
  AtomicReference<Cache> parent;
  private AtomicReferenceArray<Integer> misses;
  
  CacheNode(Cache parent, int level) {
    this.level = new AtomicInteger(level);
    int len = availableProcessors * Math.min(16, level);
    misses = new AtomicReferenceArray<>(1);
    misses.getAndSet(0, 0);
    sample = new AtomicIntegerArray(len);
    this.parent = new AtomicReference<>(parent);

    for (int i = 0; i < len; i++) {
      // misses.getAndSet(i, 0);
      sample.getAndSet(i, 0);
    }
  }
  @Atomic
  int getPos() {
    long id = Thread.currentThread().getId();
    return (Math.toIntExact(id ^ (id >>> 16)) & (misses.length() - 1));
  }
  @Atomic
  void resetSample() {
    int len = sample.length();
    for (int i = 0; i < len; i++) {
      sample.getAndSet(i, 0);
    }
  }
  @Atomic
  int approximateMissCount() {
    return misses.get(0);
  }
  @Atomic
  void resetMissCount() {
    misses.getAndSet(0, 0);
  }
  @Atomic
  void bumpMissCount() {
    misses.getAndSet(0, misses.get(0) + 1);
  }
}