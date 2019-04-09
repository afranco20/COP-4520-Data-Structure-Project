import java.util.concurrent.atomic.AtomicReference;

public class SNode {
  int hash;
  Object key;
  Object value;
  AtomicReference<GenNode> txn;

  SNode(int hash, Object key, Object value, GenNode type) {
    this.hash = hash;
    this.key = key;
    this.value = value;
    this.txn = new AtomicReference<>(type);
  }
}