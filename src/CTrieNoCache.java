import java.util.ArrayDeque;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class CTrieNoCache {
  private static final String ANODE = "ANODE";
  private static final String ENODE = "ENODE";
  private static final String FNODE = "FNODE";
  private static final String FSNODE = "FSNODE";
  private static final String FVNODE = "FVNODE";
  private static final String NOTXN = "NOTXN";
  private static final String SNODE = "SNODE";

  GenNode root;

  CTrieNoCache() {
    root = new GenNode(16);
  }

  int hash(long k) {
    return 6;
  }

  Object lookup(long k, int hash) {
    SNode result = lookup(k, hash, 0, (ANode) root.node);
    if (result != null) {
      return result.value;
    } else {
      return null;
    }
  }

  SNode lookup(long key, int hash, int level, ANode curr) {
    int pos = ((hash >>> level) & ((curr.array).length() - 1));
    GenNode old = curr.array.get(pos);

    // Not sure about "old == FVNODE"
    if (old == null || (old.nodeType.equals(FNODE) && ((FNode) old.node).frozen == null))
      return null;
    else if (old.nodeType.equals(ANODE))
      return lookup(key, hash, level + 4, (ANode) old.node);
    else if (old.nodeType.equals(SNODE)) {
      if (((SNode) old.node).key == key)
        return (SNode) old.node;
      else
        return null;
    } else if (old.nodeType.equals(ENODE)) {
      ANode an = ((ANode) ((ENode) old.node).narrow.node);
      return lookup(key, hash, level + 4, an);
    } else if (old.nodeType.equals(FNODE)) {
      if (((FNode) old.node).AorS == ANODE)
        return lookup(key, hash, level + 4, (ANode) ((FNode) old.node).frozen.node);
      else if (((FNode) old.node).AorS == SNODE) {
        GenNode fnode = ((FNode) old.node).frozen;
        if (((SNode) fnode.node).key == key)
          return (SNode) fnode.node;
        else
          return null;
      } else
        return null;
    } else {
      System.out.println("Something went wrong");
      return null;
    }
  }

  void copy(GenNode og, GenNode newOg, int level) {
    int i = 0;
    while (i < ((ANode) og.node).array.length()) {
      GenNode ent = ((ANode) og.node).array.get(i);
      if (ent.nodeType.equals(SNODE)) {
        // int pos = (ent.node.hash >>> level) & (newOg.node.array.length() - 1);
        // insertANode definition
        insertANode(newOg, ent, level);
      }
      // Assume ANodes keep the same position, if collision, just insert in the ANode
      i++;
    }
  }

  void freeze(GenNode curr) {
    int i = 0;
    while (i < ((ANode) curr.node).array.length()) {
      GenNode node = ((ANode) curr.node).array.get(i);
      if (node == null)
        if (!((ANode) curr.node).array.compareAndSet(i, node, new GenNode(null)))
          i -= 1;
        else if (node.nodeType.equals(SNODE)) {
          GenNode txn = ((SNode) node.node).txn.get();
          if (txn == null)
            if (!((SNode) node.node).txn.compareAndSet(null, node))
              i -= 1;
            else {
              ((ANode) curr.node).array.compareAndSet(i, node, txn);
              i -= 1;
            }
        } else if (node.node == ANODE) {
          GenNode fn = new GenNode((GenNode) node.node);
          ((ANode) curr.node).array.compareAndSet(i, node, fn);
          i -= 1;
        } else if (node.nodeType.equals(FNODE)) {
          if (((FNode) node.node).AorS == ANODE)
            freeze(((FNode) node.node).frozen);
        } else if (node.nodeType.equals(ENODE)) {
          completeExpansion(node);
          i -= 1;
        }

      i += 1;
    }
  }

  void completeExpansion(GenNode en) {
    freeze(((ENode) en.node).narrow);
    GenNode wide = new GenNode(16);
    copy(((ENode) en.node).narrow, wide, ((ENode) en.node).level);
    if (!((ENode) en.node).wide.compareAndSet(null, wide))
      wide = ((ENode) en.node).wide.get();
    ((ANode) (((ENode) en.node).parent.get().node))
        .array.compareAndSet(((ENode) en.node).parentpos, en, wide);
  }

  // Insert in ANode of size 16 or 4
  void insertANode(GenNode aNode, GenNode item, int level) {
  	if(!item.nodeType.equals(FNODE))
  	{
	  	if(item.nodeType.equals(SNODE)) {
	  		int pos = (((SNode)item.node).hash >>> level) & (((ANode)aNode.node).array.length() - 1);
	  		// Fail to put in array
	  		if(!((ANode)aNode.node).array.compareAndSet(pos, null, item))
	  			System.out.println("Failed to rehash item");
	  	}
	  	else if(item.nodeType.equals(ANODE)) {
	  		System.out.println("How did that happen lol");
	  	}
	  	else if(item.nodeType.equals(ENODE)) {
	  		completeExpansion(item);
	  	}
  	}
  	else {
  		GenNode fNode = ((FNode)item.node).frozen;
  		if(fNode.nodeType.equals(SNODE)) {
	  		int pos = (((SNode)fNode.node).hash >>> level) & (((ANode)aNode.node).array.length() - 1);
	  		// Fail to put in array
	  		if(!((ANode)aNode.node).array.compareAndSet(pos, null, item))
	  			System.out.println("Failed to frozen rehash item");
	  	}
	  	else if(fNode.nodeType.equals(ANODE)) {
	  		System.out.println("How did that fnode happen lol");
	  	}
  	}

  }

  GenNode createNarrow(GenNode first, GenNode second, int level) {
    GenNode narrow = new GenNode(4);
    insertANode(narrow, first, level);
    insertANode(narrow, second, level);
    return narrow;
  }

  boolean insert(long k, Object v, int h, int lev, GenNode curr, GenNode prev) {
    int pos = (h >>> lev) & (((ANode) curr.node).array.length() - 1);
    GenNode old = ((ANode) curr.node).array.get(pos);
    if (old == null) {
      GenNode sn = new GenNode(h, k, v, null);
      if (((ANode) curr.node).array.compareAndSet(pos, null, sn))
        return true;
      else
        return insert(k, v, h, lev, curr, prev);
    } else if (old.nodeType.equals(ANODE))
      return insert(k, v, h, lev + 4, old, prev);
    else if (old.nodeType.equals(SNODE)) {
      GenNode txn = ((SNode) old.node).txn.get();
      if (txn == null) {
        if (((SNode) old.node).key == k) {
          GenNode sn = new GenNode(h, k, v, null);
          if (((SNode) old.node).txn.compareAndSet(null, sn)) {
            ((ANode) curr.node).array.compareAndSet(pos, old, sn);
            return true;
          } else
            return insert(k, v, h, lev, curr, prev);
        } else if (((ANode) curr.node).array.length() == 4) {
          int ppos = (h >>> (lev - 4)) & (((ANode) prev.node).array.length() - 1);
          GenNode en = new GenNode(prev, ppos, curr, h, lev);
          if (((ANode) prev.node).array.compareAndSet(ppos, curr, en)) {
            completeExpansion(en);
            GenNode wide = ((ENode) en.node).wide.get();
            return insert(k, v, h, lev, wide, prev);
          } else
            return insert(k, v, h, lev, curr, prev);
        } else {
          GenNode sn = new GenNode(h, k, v, null);
          GenNode an = createNarrow(old, sn, lev + 4);
          if (((SNode) old.node).txn.compareAndSet(null, an)) {
            ((ANode) curr.node).array.compareAndSet(pos, old, an);
            return true;
          } else
            return insert(k, v, h, lev, curr, prev);
        }
      } else {
        ((ANode) curr.node).array.compareAndSet(pos, old, txn);
        return insert(k, v, h, lev, curr, prev);
      }
    } else if (old.nodeType.equals(ENODE))
      completeExpansion(old);

    return false;
  }

  void insert(long key, int hash, Object val) {
  	if(!insert(key, val, hash,  0, root, null))
  		insert(key, hash, val);
  }

  // generate list of numbers with same hash
  static ArrayDeque<Integer> hashCollider() {
    ArrayDeque<Integer> num;

    // filters infinite list of integers and collects the first 100 results
    num =
        IntStream.iterate(0, i -> i + 1)
            .parallel()
            .filter(i -> i % 16 == 6)
            .limit(100)
            .boxed()
            .collect(Collectors.toCollection(ArrayDeque::new));

    // return list
    return num;
  }

  public static void main(String[] args) {
    CTrieNoCache test = new CTrieNoCache();

//    test.insert(123, 6, "hope");
    System.out.println("--- test insertions ---");
    ArrayDeque<Integer> hashes = hashCollider();
    for (Integer i : hashes) {
      test.insert(i, 6, i);
    }

//    System.out.printf("%s\n", (String) test.lookup(123, 6));
    System.out.println("--- test lookup ---");
    for (Integer i : hashes) {
      String str = (String) test.lookup(i, 6);
      System.out.printf("%s%n", str);
    }
  }
}
