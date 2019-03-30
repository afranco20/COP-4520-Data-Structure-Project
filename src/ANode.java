import java.security.Key;
import java.util.concurrent.atomic.*;

public class ANode {
  AtomicReferenceArray<GenNode> array;
  // Object nodeType = ANODE;

  ANode(int length) {
    array = new AtomicReferenceArray<>(length);
  }
}
