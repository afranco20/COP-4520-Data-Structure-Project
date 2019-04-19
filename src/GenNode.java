
import org.deuce.Atomic;

public class GenNode {
  private static final String ANODE = "ANODE";
  private static final String ENODE = "ENODE";
  private static final String FNODE = "FNODE";
  private static final String FSNODE = "FSNODE";
  private static final String FVNODE = "FVNODE";
  private static final String NOTXN = "NOTXN";
  private static final String SNODE = "SNODE";

  Object node;
  String nodeType;

  GenNode(int length) {
    node = new ANode(length);
    nodeType = ANODE;
  }
  
  GenNode(GenNode anode) {
    node = new FNode(anode);
    nodeType = FNODE;
  }
  
  GenNode(int hash, Object key, Object value, GenNode type) {
    node = new SNode(hash, key, value, type);
    nodeType = SNODE;
  }

  GenNode(GenNode prev, int ppos, GenNode curr, int hash, int level) {
    node = new ENode(prev, ppos, curr, hash, level);
    nodeType = ENODE;
  }
}
