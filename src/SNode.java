import java.util.concurrent.atomic.*;

public class SNode extends Node {
  int hash;
  long key;
  Object value;
  AtomicReference<GenNode> txn;
  // Object nodeType = SNODE;

  SNode(int h, long k, Object val, GenNode type) {
    hash = h;
    key = k;
    value = val;
    txn = new AtomicReference<>(type);
  }
}
