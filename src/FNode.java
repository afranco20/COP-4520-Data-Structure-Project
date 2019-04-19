public class FNode {
  GenNode frozen;
  Object AorS;

  FNode(GenNode node) {
    frozen = node;
    AorS = (node != null) ? node.nodeType : null;
  }
}

