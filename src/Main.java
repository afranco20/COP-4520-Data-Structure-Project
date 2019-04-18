import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
  private static final int num_threads = 8;

  // generate list of numbers with same hash
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
  private static ArrayDeque<Integer> numbers() {
    return IntStream.rangeClosed(0, 1_000_000).boxed().collect(Collectors.toCollection(ArrayDeque::new));
  }

  public static void main(String[] args) {
    CacheTrie test = new CacheTrie();

   System.out.println("--- test insertions ---");
    ArrayDeque<Integer> hashes = hashCollider();
    //ArrayDeque<Integer> hashes = numbers();

    /*ExecutorService thread_pool = Executors.newFixedThreadPool(num_threads);

    for (int i = 0; i < 1_000_000; i++) {
      final int k = i;
      thread_pool.submit(() -> test.insert(k, k));
    }

    thread_pool.shutdown();
    System.out.println("=== test ===");*/

   for (Integer i : hashes) {
     // System.out.printf("%n*** INSERTING %d ***%n%n", i);
      test.fastInsert(i, i);
    }

    System.out.println("--- test lookup ---");
    for (Integer i : hashes) {
      //System.out.println(test.fastLookup(i));
        test.fastLookup(i);
    }

    //System.out.println("--- test trace ---");
    //test.printTrace();
    System.out.println("--- test cache ---");
    //test.printCache();
  }
}
