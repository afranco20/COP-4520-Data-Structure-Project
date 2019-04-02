import java.util.ArrayDeque;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
  // generate list of numbers with same hash
  static ArrayDeque<Integer> hashCollider() {
    ArrayDeque<Integer> hashes;

    // filters infinite list of integers and collects the first 100 results
    hashes = IntStream.iterate(0, i -> i + 1)
              .parallel()
              .filter(i -> i % 16 == 6)
              .limit(100)
              .boxed()
              .collect(Collectors.toCollection(ArrayDeque::new));

    // return list
    return hashes;
  }

  public static void main(String[] args) {
    CTrieNoCache test = new CTrieNoCache();

    System.out.println("--- test insertions ---");
    ArrayDeque<Integer> hashes = hashCollider();
    for (Integer i : hashes) {
      test.insert(i, 6, i);
    }

    System.out.println("--- test lookup ---");
    for (Integer i : hashes) {
      String str = (String) test.lookup(i, 6);
      System.out.printf("%s%n", str);
    }
  }
}
