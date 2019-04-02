import java.util.concurrent.atomic.AtomicReference;

public class SNode {
  int hash;
  long key;
  Object value;
  AtomicReference<GenNode> txn;

  SNode(int h, long k, Object val, GenNode type) {
    hash = h;
    key = k;
    value = val;
    txn = new AtomicReference<>(type);
  }
}
