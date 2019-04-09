public class CTrieNoCache {
  private static final String ANODE = "ANODE";
  private static final String ENODE = "ENODE";
  private static final String FNODE = "FNODE";
  private static final String FSNODE = "FSNODE";
  private static final String FVNODE = "FVNODE";
  private static final String NOTXN = "NOTXN";
  private static final String SNODE = "SNODE";

  GenNode root;
  private int count = 0;

  CTrieNoCache() {
    root = new GenNode(16);
  }

  // used to evenly hash object hashes
  private int hash(int k) {
    return (k ^ k >>> 16) & Integer.MAX_VALUE;
  }

  Object lookup(Object key) {
    SNode result = lookup(key, hash(key.hashCode()), 0, (ANode) root.node);
    return (result != null) ? result.value : null;
  }

  SNode lookup(Object key, int hash, int level, ANode curr) {
    int pos = ((hash >>> level) & ((curr.array).length() - 1));
    GenNode old = curr.array.get(pos);

    if (old == null) {
      return null;
    }

    switch (old.nodeType) {
      // Not sure about "old == FVNODE"
      case FNODE:
        if (((FNode) old.node).frozen == null) {
          return null;
        }

        switch ((String) ((FNode) old.node).AorS) {
          case ANODE:
            return lookup(key, hash, level + 4, (ANode) ((FNode) old.node).frozen.node);

          case SNODE:
            GenNode fnode = ((FNode) old.node).frozen;
            return (((SNode) fnode.node).key == key) ? (SNode) fnode.node : null;

          default:
            return null;
        }

      case ANODE:
        return lookup(key, hash, level + 4, (ANode) old.node);

      case SNODE:
        return (((SNode) old.node).key == key) ? (SNode) old.node : null;

      case ENODE:
        ANode an = ((ANode) ((ENode) old.node).narrow.node);
        return lookup(key, hash, level + 4, an);

      default:
        System.out.println("### ERROR IN LOOKUP! ###");
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
        transfer(newOg, ent, level);
      }
      // Assume ANodes keep the same position, if collision, just insert in the ANode
      i++;
    }
  }

  void unfreeze(GenNode arr) {
    int length = ((ANode) arr.node).array.length();

    for(int i = 0; i < length; i++) {
      GenNode curr = ((ANode) arr.node).array.get(i);

      switch (curr.nodeType) {
        case SNODE:
          GenNode txt = ((SNode) curr.node).txn.get();
          if (txt != null) {
            ((SNode) curr.node).txn.compareAndSet(txt, null);
          }
          break;

        case ANODE:
          unfreeze(curr);
          break;

        case ENODE:
          completeExpansion(curr);
          break;

        case FNODE:
          continue; // Frozen nodes from another expansion

        default:
          System.out.println("### UNKNOWN NODE TYPE! ###");
          System.exit(1);
      }
    }
  }

  void freeze(GenNode curr) {
    int i = 0;

    while (i < ((ANode) curr.node).array.length()) {
      GenNode node = ((ANode) curr.node).array.get(i);
      if (node == null){
        if (!((ANode) curr.node).array.compareAndSet(i, node, new GenNode(null))) {
          i -= 1;
        }
      }

      switch (node.nodeType) {
        case SNODE:
          GenNode txn = ((SNode) node.node).txn.get();

          if (txn == null) {
            GenNode fNode = new GenNode(node);
            ((FNode) fNode.node).AorS = SNODE;

            if (!((SNode) node.node).txn.compareAndSet(null, fNode)) {
              i -= 1;
            }
          }

          else if (!txn.nodeType.equals(FNODE)) {
            if (!((FNode) txn.node).AorS.equals(SNODE)) {
              ((ANode) curr.node).array.compareAndSet(i, node, txn);
              i -= 1;
            }
          }
          break;

        case ANODE:
          GenNode fn = new GenNode((GenNode) node.node);
          ((ANode) curr.node).array.compareAndSet(i, node, fn);
          i -= 1;
          break;

        case FNODE:
          if (((FNode) node.node).AorS.equals(ANODE)) {
            freeze(((FNode) node.node).frozen);
          }
          break;

        case ENODE:
          completeExpansion(node);
          i -= 1;
          break;

        default:
          System.out.println("### ERROR - UNKNOWN NODE TYPE! ###");
          System.exit(1);
      }

      i += 1;
    }
  }

  // Sequential transfer, also look where it is called just in case
  void transfer(GenNode og, GenNode newOg, int level) {
  	int i = 0, length = ((ANode) og.node).array.length();

  	while(i < length) {
  		GenNode node = ((ANode)og.node).array.get(i);
  		if(node == null) continue;
  		if(node.nodeType.equals(SNODE) || node.nodeType.equals(FNODE)) {
  		  GenNode check = (node.nodeType.equals(FNODE))? (node): (((SNode)node.node).txn.get());
          if((check.nodeType.equals(FNODE))) {
                if(((FNode) check.node).AorS.equals(SNODE)) {
                    GenNode sn = ((FNode) check.node).frozen;
                    int pos = (((SNode)sn.node).hash >>> level) & (16 - 1);
                    if(((ANode)newOg.node).array.get(pos) == null) {
                      ((ANode)newOg.node).array.set(pos, sn);
                    }
                    else {
                      insertWide(newOg, sn, level, pos);
                    }
                } else if(((FNode)check.node).AorS.equals(ANODE)){
              transfer(((FNode)check.node).frozen, newOg, level);
            }
            else {
              System.out.println("ANodes not frozen or LNode");
            }
          }
  		}

      i += 1;
  	}
  }

  void completeExpansion(GenNode en) {
    freeze(((ENode) en.node).narrow);
    GenNode wide = new GenNode(16);
    transfer(((ENode) en.node).narrow, wide, ((ENode) en.node).level);

    // unfreeze function
    // start at en.node.parent.array.get(en.node.parentpos), then work way down tree
    // changing all SNode.txn values to null
    unfreeze(((ENode) en.node).narrow);

    // System.out.println("--- expansion ---");
    if (!((ENode) en.node).wide.compareAndSet(null, wide)) {
      wide = ((ENode) en.node).wide.get();
    }

    ((ANode) (((ENode) en.node).parent.get().node)).array.compareAndSet(((ENode) en.node).parentpos, en, wide);
  }

  // Insert in ANode of size 16
  void insertWide(GenNode aNode, GenNode item, int level) {
  	int pos = (((SNode)item.node).hash >>> level) & (16 - 1);

  	if(((ANode)aNode.node).array.get(pos) == null) {
  		((ANode)aNode.node).array.set(pos, item);
  	}

  	else {
  		insertWide(aNode, item, level, pos);
  	}
  }

  void insertWide(GenNode aNode, GenNode item, int level, int pos) {
  	// Check what is at the current position
  	GenNode old = ((ANode)aNode.node).array.get(pos);

  	if(old.nodeType.equals(SNODE)) {
  		GenNode an = createNarrowOrWide(old, item, level + 4);
  		((ANode)aNode.node).array.set(pos, an);
  	}

  	else if(old.nodeType.equals(ANODE)) {
  		int newPos = (((SNode)item.node).hash >>> (level + 4)) & (((ANode)old.node).array.length() - 1);

  		if(((ANode)old.node).array.get(newPos) == null) {
  			((ANode)old.node).array.set(newPos, item);
  		}

  		else if(((ANode)old.node).array.length() == 4) {
  			GenNode an = new GenNode(16);
  			transfer(old, an, level + 4);
  			((ANode)aNode.node).array.set(pos, an);
  			insertWide(aNode, item, level, pos);
  		}

  		else {
  			insertWide(old, item, level + 4, newPos);
  		}
  	}

  	else {
  		System.out.println("LNode or unexpected case");
  	}
  }

  GenNode createNarrowOrWide(GenNode first, GenNode second, int level) {
  	/// Same hash, wtf is an LNode
    int pos1 = (((SNode)first.node).hash >>> level) & (4 - 1);
    int pos2 = (((SNode)second.node).hash >>> level) & (4 - 1);

    if(pos1 != pos2) {
    	GenNode narrow = new GenNode(4);
    	((ANode)narrow.node).array.set(pos1, first);
    	((ANode)narrow.node).array.set(pos2, second);
    	return narrow;
    }

    else {
    	GenNode wide = new GenNode(16);
    	insertWide(wide, first, level);
    	insertWide(wide, second, level);
    	return wide;
    }
  }

  boolean insert(Object k, Object v, int h, int lev, GenNode curr, GenNode prev) {
    // Get position in ANode according to size of ANode, level and hash code
    int pos = (h >>> lev) & (((ANode) curr.node).array.length() - 1);

    // Check what is at the current position
    GenNode old = ((ANode) curr.node).array.get(pos);



    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // DEBUG
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    System.out.printf("--- insert ---%n");
    System.out.printf("level: %d%n%n", lev);
    System.out.printf("array values:%n");
    for(int i = 0; i < ((ANode) curr.node).array.length(); i++) {
      if(((ANode) curr.node).array.get(i) != null)
        System.out.println("[" + i + "] = " + ((ANode) curr.node).array.get(i).nodeType);
      else
        System.out.println("[" + i + "] = " + "null");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////


    // If current position is empty
    if (old == null) {
      //System.out.println("Congrats");
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
      //System.out.println("Heyooo");
      return insert(k, v, h, lev + 4, old, curr);
    }

    // If we found an entry with the current hash bits
    else if (old.nodeType.equals(SNODE)) {
      // Check if it has a special value
      GenNode txn = ((SNode) old.node).txn.get();

      //System.out.println("what");
      // We're allowed to modify the node
      if (txn == null) {
        //System.out.println("whater");
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
          //System.out.println("length 4");
          GenNode en = new GenNode(prev, ppos, curr, h, lev);

          // Check that the parent of the current ANode still contains current and then insert the ENode
          if (((ANode) prev.node).array.compareAndSet(ppos, curr, en)) {
            completeExpansion(en);
            GenNode wide = ((ANode) prev.node).array.get(ppos);

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
          GenNode an = createNarrowOrWide(old, sn, lev + 4);

          // Check if nothing has changed, replace old ANode with new ANode
          if (((SNode) old.node).txn.compareAndSet(null, an)) {
            ((ANode) curr.node).array.compareAndSet(pos, old, an);

            // ??? evil bit level magic [don't remove]
            ((SNode) old.node).txn.compareAndSet(an, null);
            //System.out.println("new narrow");
            return true;
          }

          // Failed so restart
          else {
            return insert(k, v, h, lev, curr, prev);
          }
        }
      }


      // Frozen SNode so unable to make changes, returns back up to the ENode in control of this SNode
      else if (txn.nodeType.equals(FNODE) && ((FNode)txn.node).frozen.nodeType.equals(SNODE)) {
        return false;
      }

      // If SNode or ANode, help complete concurrent insertion
      else {
        // Help update cur[pos] to value at txn
        //System.out.println("hey");
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
  void insert(Object key, Object val) {
  	if(!insert(key, val, hash(key.hashCode()),  0, root, null)) {
  	  if(count >= 3) {
        System.out.printf("Cannot insert: %d %n", hash(key.hashCode()));
        return;
      }
  	  count++;
      insert(key, val);
    }

  	count = 0;
  }

  void printTrace(GenNode array) {
    int length = ((ANode)array.node).array.length();

    for(int i = 0; i < length; i++) {
      GenNode item = ((ANode)array.node).array.get(i);

      if(item == null) {
        continue;
      }

      switch (item.nodeType) {
        case SNODE:
          System.out.println(((SNode)item.node).value);
          break;

        case ANODE:
          printTrace(item);
          break;

        case FNODE:
          GenNode fr = ((FNode) item.node).frozen;
          switch ((String) ((FNode) item.node).AorS) {
            case ANODE:
              printTrace(fr);
              break;

            case SNODE:
              System.out.println(((SNode) fr.node).value);
              break;

            default:
              continue;
          }

          break;

        case ENODE:
          printTrace(((ENode) item.node).narrow);

        default:
          System.out.println("### UNKNOWN NODE TYPE! ###");
          System.exit(1);
      }
    }
  }

  void printTrace() {
    printTrace(root);
  }
}
