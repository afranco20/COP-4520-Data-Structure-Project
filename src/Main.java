import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.deuce.Atomic;

public class Main {
  private static final int num_threads = 8;

  // generate list of numbers with same hash
  @Atomic
  private static ArrayDeque<Integer> hashCollider() {
    ArrayDeque<Integer> hashes;

    // filters infinite list of integers and collects the first 100 results
    hashes = IntStream.iterate(0, i -> i + 1)
        .parallel()
        .filter(i -> i % 16 == 6)
        .limit(100000)
        .boxed()
        .collect(Collectors.toCollection(ArrayDeque::new));

    // return list
    return hashes;
  }

  // generate list of sequential numbers
  @Atomic
  private static ArrayDeque<Integer> numbers() {
    return IntStream.rangeClosed(0, 1_000_000).boxed().collect(Collectors.toCollection(ArrayDeque::new));
  }
  @Atomic
  public static void main(String[] args) {
    CacheTrie test = new CacheTrie();

    System.out.println("--- test insertions ---");
    ArrayDeque<Integer> hashes = hashCollider();
    //ArrayDeque<Integer> hashes = numbers();

    ExecutorService thread_pool = Executors.newFixedThreadPool(num_threads);

    for (int i = 0; i < 1_000_000; i++) {
      final int k = i;
      thread_pool.submit(() -> test.fastInsert(k, k));
      thread_pool.submit(() -> test.fastLookup(k));
    }

    thread_pool.shutdown();
    System.out.println("=== test ===");

    /*for (Integer i : hashes) {
      // System.out.printf("%n*** INSERTING %d ***%n%n", i);
//      test.insert(i, i);
      test.fastInsert(i, i);
    }

    System.out.println("--- test lookup ---");
    for (Integer i : hashes) {
      //System.out.println(test.fastLookup(i));
//      test.lookup(i);
      test.fastLookup(i);
    }

    //System.out.println("--- test trace ---");
    //test.printTrace();

    System.out.println("--- test cache ---");
    //test.printCache();*/
  }
}
