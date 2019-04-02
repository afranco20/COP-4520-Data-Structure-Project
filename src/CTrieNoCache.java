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
    // Get position in ANode according to size of ANode, level and hash code
    int pos = (h >>> lev) & (((ANode) curr.node).array.length() - 1);

    // Check what is at the current position
    GenNode old = ((ANode) curr.node).array.get(pos);

    // If current position is empty
    if (old == null) {
      GenNode sn = new GenNode(h, k, v, null);

      // Check nothing has changed, then insert
      if (((ANode) curr.node).array.compareAndSet(pos, null, sn)) {
        return true;
      }

      // Collision, restart at next level
      else {
        return insert(k, v, h, lev, curr, prev);
      }
    }

    // If there is another array at position, jump to that array
    else if (old.nodeType.equals(ANODE)) {
      return insert(k, v, h, lev + 4, old, prev);
    }

    // If we found an entry with the current hash bits
    else if (old.nodeType.equals(SNODE)) {
      // Check if it has a special value
      GenNode txn = ((SNode) old.node).txn.get();

      // We're allowed to modify the node
      if (txn == null) {

        // If we want to insert a node with the same key
        if (((SNode) old.node).key == k) {
          GenNode sn = new GenNode(h, k, v, null);

          // Check if nothing has changed and announce that there is new SNode
          if (((SNode) old.node).txn.compareAndSet(null, sn)) {
            ((ANode) curr.node).array.compareAndSet(pos, old, sn);
            return true;
          }

          // Failed to announce there is new SNode so restart
          else {
            return insert(k, v, h, lev, curr, prev);
          }
        }

        // Check if ANode is narrow, create ENode to announce expansion
        else if (((ANode) curr.node).array.length() == 4) {
          // Save the position in the parent
          int ppos = (h >>> (lev - 4)) & (((ANode) prev.node).array.length() - 1);

          GenNode en = new GenNode(prev, ppos, curr, h, lev);

          // Check that the parent of the current ANode still contains current and then insert the ENode
          if (((ANode) prev.node).array.compareAndSet(ppos, curr, en)) {
            completeExpansion(en);
            GenNode wide = ((ENode) en.node).wide.get();

            // Restart insertion with the wide ANode in the new ENode
            return insert(k, v, h, lev, wide, prev);
          }

          // Failed to insert ENode so restart
          else {
            return insert(k, v, h, lev, curr, prev);
          }
        }

        // If ANode is already wide
        else {
          GenNode sn = new GenNode(h, k, v, null);

          // Creates a new ANode with the new and old SNode
          GenNode an = createNarrow(old, sn, lev + 4);

          // Check if nothing has changed, replace old ANode with new ANode
          if (((SNode) old.node).txn.compareAndSet(null, an)) {
            ((ANode) curr.node).array.compareAndSet(pos, old, an);
            return true;
          }

          // Failed so restart
          else {
            return insert(k, v, h, lev, curr, prev);
          }
        }
      }


      // Frozen SNode so unable to make changes, returns back up to the ENode in control of this SNode
      else if (txn.nodeType.equals(FSNODE)) {
        return false;
      }

      // If SNode or ANode, help complete concurrent insertion
      else {
        // Help update cur[pos] to value at txn
        ((ANode) curr.node).array.compareAndSet(pos, old, txn);
        return insert(k, v, h, lev, curr, prev);
      }
    }

    // Help concurrent expansion
    else if (old.nodeType.equals(ENODE)) {
      completeExpansion(old);
    }

    // If FVNode or FNode
    return false;
  }

  // Initial call
  void insert(long key, int hash, Object val) {
  	if(!insert(key, val, hash,  0, root, null)) {
      insert(key, hash, val);
    }
  }
}
