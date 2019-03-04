import java.security.Key;
import java.util.concurrent.atomic.*;

public class CTrieNoCache<T> {
	
	private static final Object NoTxn = "NoTxn";
	private static final Object FSNode = "FSNode";
	private static final Object FVNode = "FVNode";

	static class SNode<R> {
		int hash;
		Key key;
		R value;
		Object txn;
	}

	static class ANode {
		Object [] array;
	}

	static class ENode {
		ANode parent;
		int parentpos;
		ANode narrow;
		int hash;
		int level;
		ANode wide;
	}

	static class FNode {
		ANode frozen;
	}

	ANode root;

	CTrieNoCache () {
		root = new ANode();
		root.array = new Object[16];
	}

	Object READ (ANode curr, int pos) {
		AtomicReferenceArray refCurr = new AtomicReferenceArray(curr.array);
		return refCurr.get(pos);
	}

	Object lookup(Key key, int hash, int level, ANode curr) {
		int pos = ((hash >>> level) & ((curr.array).length - 1));
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

	public static void main(String[] args) {
		if("NoTx" == NoTxn)
			System.out.println("Hello World");	
	}
}