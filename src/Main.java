import java.util.ArrayDeque;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
  // generate list of numbers with same hash
  private static ArrayDeque<Integer> hashCollider() {
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

  // generate list of sequential numbers
  private static ArrayDeque<Integer> numbers() {
    return IntStream.rangeClosed(0, 100).boxed().collect(Collectors.toCollection(ArrayDeque::new));
  }

  public static void main(String[] args) {
    CTrieNoCache test = new CTrieNoCache();

    System.out.println("--- test insertions ---");
    ArrayDeque<Integer> hashes = hashCollider();
//    ArrayDeque<Integer> hashes = numbers();

    int count = 0;
    for (Integer i : hashes) {
      System.out.printf("%n*** INSERTING %d ***%n%n", i);
      test.insert(count++, i);
    }

    System.out.println("--- test lookup ---");
    count--;
    while (count >= 0) {
      System.out.println(test.lookup(count--));
    }
//    for (Integer i : hashes) {
//      System.out.println(test.lookup(i));
//    }

    System.out.println("--- test trace ---");
    test.printTrace();
  }
}
