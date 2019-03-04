import java.security.Key;
import java.util.concurrent.atomic.*;

public class CTrieNoCache {
	
	private static final Object NoTxn = "NoTxn";
	private static final Object FSNode = "FSNode";
	private static final Object FVNode = "FVNode";

	static class SNode {
		int hash;
		Key key;
		Object value;
		AtomicReference<Object> txn;

		SNode (int h, Key k, Object val, Object type) {
			hash = h;
			key = k;
			value = val;
			txn = new AtomicReference<Object>(type);
		}
	}

	static class ANode {
		AtomicReferenceArray<Object> array;
		ANode (int length) {
			array = new AtomicReferenceArray<Object>(length);
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
		ANode wide = new ANode(16);
		return populateWide(wide, old, newVal, bothLev);
	}

	// Not sure about making parent and wide atomic, if so might need narrow atomic as well
	static class ENode {
		AtomicReference<ANode> parent;
		int parentpos;
		ANode narrow;
		int hash;
		int level;
		AtomicReference<ANode> wide;
		ENode(ANode prev, int ppos, ANode curr, int h, int lev) {
			parent = new AtomicReference<ANode>(prev);
			parentpos = ppos;
			narrow = curr;
			hash = h;
			level = lev;
		}
	}

	static class FNode {
		ANode frozen;
		FNode(ANode node) {
			frozen = node;
		}
	}

	ANode root;

	CTrieNoCache () {
		root = new ANode(16);
	}

	Object READ (ANode curr, int pos) {
		AtomicReferenceArray<Object> refCurr = curr.array;
		return refCurr.get(pos);
	}


	Object lookup(Key key, int hash, int level, ANode curr) {
		int pos = ((hash >>> level) & ((curr.array).length() - 1));
		Object old = READ(curr, pos);

		// Not sure about "old == FVNode"
		if(old == null || old == FVNode)
			return null;
		else if(old instanceof ANode)
			return lookup(key, hash, level + 4, (ANode) old);
		else if(old instanceof SNode) {
			if(((SNode)old).key == key)
				return ((SNode)old).value;
			else
				return null;
		}
		else if(old instanceof ENode) {
			ANode an = ((ENode)old).narrow;
			return lookup(key, hash, level + 4, an);
		}
		else if(old instanceof FNode)
			return lookup(key, hash, level + 4, ((FNode) old).frozen);
		else
		{
			System.out.println("Something went wrong");
			return null;
		}
	}

	// ?????????
	void copy(ANode narrow, ANode wide, int lev) {

	}

	void freeze(ANode curr) {
		int i = 0;
		while(i < curr.array.length()) {
			Object node = curr.array.get(i);
			if(node == null) 
				if(!curr.array.compareAndSet(i, node, FVNode))
					i -= 1;
			else if(node instanceof SNode) {
				Object txn = ((SNode)node).txn.get();
				if(txn == NoTxn)
					if(!((SNode)node).txn.compareAndSet(NoTxn, FSNode))
						i -= 1;
				else if(txn != FSNode) {
					curr.array.compareAndSet(i, node, txn);
					i -= 1;
				}
			}
			else if(node instanceof ANode) {
				FNode fn = new FNode((ANode)node);
				curr.array.compareAndSet(i, node, fn);
				i -= 1;
			}
			else if(node instanceof FNode)
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
		ANode wide = new ANode(16);
		copy(en.narrow, wide, en.level);

		if(!en.wide.compareAndSet(null, wide))
			wide = en.wide.get();
		(en.parent.get()).array.compareAndSet(en.parentpos, en, wide);
	}

	boolean insert(Key k, Object v, int h, int lev, ANode curr, ANode prev) {
		int pos = ((h >>> lev) & (curr.array.length() - 1));
		Object old = READ(curr, pos);

		if(old == null) {
			SNode sn = new SNode(h, k, v, NoTxn);
			
			if(curr.array.compareAndSet(pos, old, sn))
				return true;
			else
				return insert(k, v, h, lev + 4, (ANode)old, curr);
		}

		else if(old instanceof ANode)
			return insert(k, v, h, lev + 4, (ANode) old, curr);

		else if(old instanceof SNode) {
			Object txn = ((SNode)old).txn.get();
			if(txn == NoTxn) {
				if(((SNode)old).key == k) {
					SNode sn = new SNode(h, k, v, NoTxn);
					if(((SNode)old).txn.compareAndSet(NoTxn, sn)) {
						curr.array.compareAndSet(pos, (SNode)old, sn);
						return true;
					}
					else
						return insert(k, v, h, lev, curr, prev);
				}
				else if(curr.array.length() == 4) {
					int ppos = ((h >>> (lev - 4)) & (prev.array.length() - 1));
					ENode en = new ENode(prev, ppos, curr, h, lev);

					if(prev.array.compareAndSet(ppos, curr, en)) {
						
						completeExpansion(en);
						ANode wide = en.wide.get();
						
						return insert(k, v, h, lev, wide, prev);
					}
					else
						return insert(k, v, h, lev, curr, prev);
				}
				else {
					
					SNode sn = new SNode(h, k, v, NoTxn);
					ANode an = createWide((SNode)old, sn, lev + 4);

					if(((SNode)old).txn.compareAndSet(NoTxn, an)) {
						curr.array.compareAndSet(pos, old, an);
						return true;
					}
					else
						return insert(k, v, h, lev, curr, prev);
				}
			}
			else if(txn == FSNode)
				return false;
			else {
				curr.array.compareAndSet(pos, old, txn);
				return insert(k, v, h, lev, curr, prev);
			}
		}
		else if(old instanceof ENode)
			completeExpansion((ENode)old);
		return false; 
	}

	public static void main(String[] args) {
		if("NoTx" == NoTxn)
			System.out.println("Hello World");	
	}
}