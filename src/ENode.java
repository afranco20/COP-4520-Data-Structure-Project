import java.security.Key;
import java.util.concurrent.atomic.*;

// Not sure about making parent and wide atomic, if so might need narrow atomic as well
public class ENode extends Node {
  AtomicReference<GenNode> parent;
  int parentpos;
  GenNode narrow;
  int hash;
  int level;
  AtomicReference<GenNode> wide;
  // Object nodeType = ENODE;

  ENode(GenNode prev, int ppos, GenNode curr, int h, int lev) {
    parent = new AtomicReference<GenNode>(prev);
    parentpos = ppos;
    narrow = curr;
    hash = h;
    level = lev;
  }
}
