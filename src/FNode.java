import java.security.Key;
import java.util.concurrent.atomic.*;

public class FNode extends Node {
  GenNode frozen;
  Object AorS;
  // Object nodeType = FNODE;
  FNode(GenNode node) {
    frozen = node;
    if (node != null)
      AorS = node.nodeType;
    else
      AorS = null;
  }
}
