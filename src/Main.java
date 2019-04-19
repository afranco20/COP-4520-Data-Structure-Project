import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class Main {
  // generate list of sequential numbers
  private static ArrayDeque<Integer> generate() {
    ArrayDeque<Integer> list = new ArrayDeque<>();

    IntStream.iterate(0, i -> i + 1)
        .limit(1_000_000)
        .forEach(i -> list.add(ThreadLocalRandom.current().nextInt()));

    return list;
  }

  public static void main(String[] args) {
    CacheTrie cache_trie = new CacheTrie();

    System.out.println("--- test generate ---");
    ArrayDeque<Integer> list = generate();

    ExecutorService thread_pool;
    long startTime;
    long endTime;

    for (int num_threads = 1; num_threads <= 32; num_threads <<= 1) {
      thread_pool = Executors.newFixedThreadPool(num_threads);
      List<Callable<Object>> tasks = new ArrayList<>();

      list.forEach(i -> {
        tasks.add(() -> {
          cache_trie.fastInsert(i, i);
          return null;
        });
      });

      startTime = System.nanoTime();
      try {
        thread_pool.invokeAll(tasks);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      thread_pool.shutdown();
      endTime = System.nanoTime();

      System.out.printf("%d, %f%n", num_threads, ((double) (endTime - startTime) / 1E6));
    }
  }
}
