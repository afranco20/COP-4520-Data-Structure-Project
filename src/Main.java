import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import org.deuce.Atomic;

public class Main {
  static CacheTrie cacheTrie;

   // generate list of random numbers
  @Atomic
 private static List<Integer> generate(int num_nodes) {
    List<Integer> list = new Vector<>(num_nodes);

    IntStream.iterate(0, i -> i + 1)
        .limit(num_nodes)
        .parallel()
        .forEach(i -> list.add(ThreadLocalRandom.current().nextInt()));

    return list;
  }

  public static void main(String[] args) {
    benchmark(250_000);
//    benchmark(500_000);
//    benchmark(1_000_000);
  }

  private static void benchmark(int num_nodes) {
    List<Integer> list = generate(num_nodes);
    float[] v1 = {.50f, .75f, .25f};
    float[] v2 = {.50f, .25f, .75f};

    for (int i = 0; i < 3; i++) {
      System.out.printf("Distribution (%.0f%% / %.0f%%)%n", (v1[i] * 100), (v2[i] * 100));

      for (int num_threads = 1; num_threads <= 8; num_threads <<= 1) {
        cacheTrie = new CacheTrie();
        distribution(list, num_threads, (int) (num_nodes * v1[i]), (int) (num_nodes * v2[i]));
    }
    }
  }
  @Atomic
  private static void distribution(List<Integer> list, int num_threads, int push, int pop) {
    ExecutorService thread_pool;
    long startTime;
    long endTime;

    List<Callable<Object>> insert_tasks = new ArrayList<>(push);
    List<Callable<Object>> lookup_tasks = new ArrayList<>(pop);

    IntStream.range(0, push).forEach(i -> {
      int temp = list.get(i);

      insert_tasks.add(() -> {
        cacheTrie.fastInsert(temp, temp);
        return null;
      });
    });

    IntStream.range(0, pop).forEach(i -> {
      int temp = list.get(i % push);

      lookup_tasks.add(() -> {
        cacheTrie.fastLookup(temp);
        return null;
      });
    });

    startTime = System.nanoTime();
    thread_pool = Executors.newFixedThreadPool(num_threads);

    try {
      thread_pool.invokeAll(insert_tasks);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    try {
      thread_pool.invokeAll(lookup_tasks);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    thread_pool.shutdown();
    endTime = System.nanoTime();

    System.out.printf("%d, %f%n", num_threads, ((double) (endTime - startTime) / 1E6));
  }
}
