import java.util.concurrent.atomic.AtomicReference;

// Not sure about making parent and wide atomic, if so might need narrow atomic as well
public class ENode {
  AtomicReference<GenNode> parent;
  int parentpos;
  GenNode narrow;
  int hash;
  int level;
  AtomicReference<GenNode> wide;

  ENode(GenNode prev, int ppos, GenNode curr, int h, int lev) {
    parent = new AtomicReference<>(prev);
    parentpos = ppos;
    narrow = curr;
    hash = h;
    level = lev;
  }
}
