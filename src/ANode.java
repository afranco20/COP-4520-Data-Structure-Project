import java.util.concurrent.atomic.AtomicReferenceArray;

public class ANode {
  AtomicReferenceArray<GenNode> array;

  ANode(int length) {
    array = new AtomicReferenceArray<>(length);
  }
}
