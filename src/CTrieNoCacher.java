import java.security.Key;
import java.util.concurrent.atomic.*;

public class CTrieNoCacher {
	
	private static final Object NOTXN = "NOTXN";
	private static final Object FSNODE = "FSNODE";
	private static final Object FVNODE = "FVNODE";
	private static final Object ANODE = "ANODE";
	private static final Object SNODE = "SNODE";
	private static final Object ENODE = "ENODE";
	private static final Object FNODE = "FNODE";

	class GenNode {
		Object node;
		Object nodeType;
		GenNode(int length) {
			node = new ANode(length);
			nodeType = ANODE;
		}
		GenNode(ANode anode) {
			node = new FNode(anode);
			nodeType = FNODE;
		}
		GenNode(int hash, Key key, Object value, Object type) {
			node = new SNode(hash, key, value, type);
			nodeType = SNODE;
		}
		GenNode(ANode prev, int ppos, ANode curr, int hash, int level) {
			node = new ENode(prev, ppos, curr, hash, level);
			nodeType = ENODE;
		}

	}

	// ????????
	ANode populateWide(ANode wide, SNode old, SNode newVal, int lev) {
		int oldPos = ((old.hash >>> lev) & (wide.array.length() - 1));
		int newValPos = ((newVal.hash >>> lev) & (wide.array.length() - 1));
		wide.array.getAndSet(oldPos, old);
		wide.array.getAndSet(newValPos, newVal);
		return wide;
	}

	ANode createWide(SNode old, SNode newVal, int bothLev) {
		GenNode wide = new GenNode(16);
		return populateWide(wide, old, newVal, bothLev);
	}

	GenNode root;

	CTrieNoCacher () {
		root = new GenNode(16);
	}

	GenNode READ (GenNode curr, int pos) {
		AtomicReferenceArray<GenNode> refCurr = ((ANode)curr.node).array;
		return ((GenNode) refCurr.get(pos));
	}

	T lookupInit(Key k) {
		SNode result = lookup(k, hash(k), 0, (ANode) root.node);
		if(result != null)
			return result.value;
		else
			return null;
	}

	SNode lookup(Key key, int hash, int level, ANode curr) {
		int pos = ((hash >>> level) & ((curr.array).length() - 1));
		GenNode old = READ(curr, pos);

		// Not sure about "old == FVNODE"
		if(old == null || old.nodeType == FVNODE)
			return null;
		else if(old.nodeType == ANODE)
			return lookup(key, hash, level + 4, (ANode) old.node);
		else if(old.nodeType == SNODE) {
			if(((SNode)old.node).key == key)
				return (SNode)old.node;
			else
				return null;
		}
		else if(old.nodeType == ENODE) {
			ANode an = ((ENode)old.node).narrow;
			return lookup(key, hash, level + 4, an);
		}
		else if(old.nodeType == FNODE)
			return lookup(key, hash, level + 4, ((FNode) old.node).frozen);
		else
		{
			System.out.println("Something went wrong");
			return null;
		}
	}

	// ?????????
	void copy(ANode narrow, ANode wide, int lev) {

	}

	// Need to fix, ANodes array may need to be composed of GenNodes since there is no
	// way to determine if something is an ANode or SNode, unless make yet another wrapper class
	// for only ANode array entries
	// Txn incorporated in genNode node type field
	void freeze(ANode curr) {
		int i = 0;
		while(i < curr.array.length()) {
			GenNode node = new GenNode(curr.array.get(i));
			if(node == null) 
				if(!curr.array.compareAndSet(i, node, FVNODE))
					i -= 1;
			else if(node.nodeType == SNode) {
				Object txn = ((SNode)node.node).txn.get();
				if(txn == NOTXN)
					if(!((SNode)node.node).txn.compareAndSet(NOTXN, FSNODE))
						i -= 1;
				else if(txn != FSNODE) {
					curr.array.compareAndSet(i, node, txn);
					i -= 1;
				}
			}
			else if(node.node == ANODE) {
				GenNode fn = new GenNode((ANode)node.node);
				curr.array.compareAndSet(i, node, fn);
				i -= 1;
			}
			else if(node.nodeType == FNode)
				freeze(((FNode)node).frozen);
			else if(node instanceof ENode) {
				completeExpansion((ENode)node);
				i -= 1;
			}

			i += 1; 
		}
	}

	void completeExpansion(ENode en) {
		freeze(en.narrow);
		GenNode wide = new GenNode(16);
		copy(en.narrow, (ANode)wide.node, en.level);

		if(!en.wide.compareAndSet(null, (ANode)wide.node))
			wide.node = en.wide.get();
		(en.parent.get()).array.compareAndSet(en.parentpos, en, (ANode)wide.node);
	}

	void initInsert(Key k, T v) {

		if(!insert(k, v, hash(k), 0, (ANode) root.node, null))
			insert(k, v);
	}

	boolean insert(Key k, Object v, int h, int lev, ANode curr, ANode prev) {
		int pos = ((h >>> lev) & (curr.array.length() - 1));
		GenNode old = READ(curr, pos);

		if(old == null) {
			GenNode sn = new GenNode(h, k, v, NOTXN);
			
			if(curr.array.compareAndSet(pos, (ANode)old, (SNode)sn.node))
				return true;
			else
				return insert(k, v, h, lev + 4, (ANode)old, curr);
		}

		else if(old.nodeType == ANODE)
			return insert(k, v, h, lev + 4, (ANode) old.node, curr);

		else if(old.nodeType == SNODE) {
			Object txn = ((SNode)old.node).txn.get();
			if(txn == NOTXN) {
				if(((SNode)old.node).key == k) {
					GenNode sn = new GenNode(h, k, v, NOTXN);
					if(((SNode)old.node).txn.compareAndSet(NOTXN, (SNode)sn.node)) {
						curr.array.compareAndSet(pos, (SNode)old.node, (SNode) sn.node);
						return true;
					}
					else
						return insert(k, v, h, lev, curr, prev);
				}
				else if(curr.array.length() == 4) {
					int ppos = ((h >>> (lev - 4)) & (prev.array.length() - 1));
					GenNode en = new GenNode(prev, ppos, curr, h, lev);

					if(prev.array.compareAndSet(ppos, curr, (ENode)en.node)) {
						
						completeExpansion((ENode) en.node);
						ANode wide = ((ENode) en.node).wide.get();
						
						return insert(k, v, h, lev, wide, prev);
					}
					else
						return insert(k, v, h, lev, curr, prev);
				}
				else {
					
					GenNode sn = new GenNode(h, k, v, NOTXN);
					ANode an = createWide((SNode)old.node, (SNode) sn.node, lev + 4);

					if(((SNode)old.node).txn.compareAndSet(NOTXN, an)) {
						curr.array.compareAndSet(pos, (SNode)old.node, an);
						return true;
					}
					else
						return insert(k, v, h, lev, curr, prev);
				}
			}
			else if(txn == FSNODE)
				return false;
			else {
				curr.array.compareAndSet(pos, (SNode)old.node, txn);
				return insert(k, v, h, lev, curr, prev);
			}
		}
		else if(old.nodeType == ENODE)
			completeExpansion((ENode)old.node);
		return false; 
	}

	public static void main(String[] args) {
		if("NoTx" == NOTXN)
			System.out.println("Hello World");	
	}
}