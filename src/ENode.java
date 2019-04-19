import java.util.concurrent.atomic.AtomicReference;
import org.deuce.Atomic;

// Not sure about making parent and wide atomic, if so might need narrow atomic as well
public class ENode {
  AtomicReference<GenNode> parent;
  int parentpos;
  GenNode narrow;
  int hash;
  int level;
  AtomicReference<GenNode> wide;
  ENode(GenNode prev, int ppos, GenNode curr, int hash, int level) {
    this.parent = new AtomicReference<>(prev);
    this.parentpos = ppos;
    this.narrow = curr;
    this.hash = hash;
    this.level = level;
    this.wide = new AtomicReference<>(null);
  }
}
