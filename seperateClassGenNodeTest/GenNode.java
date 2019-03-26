import java.security.Key;

public class GenNode {

	private static final Object NOTXN = "NOTXN";
	private static final Object FSNODE = "FSNODE";
	private static final Object FVNODE = "FVNODE";
	private static final Object ANODE = "ANODE";
	private static final Object SNODE = "SNODE";
	private static final Object ENODE = "ENODE";
	private static final Object FNODE = "FNODE";

	Object node;
	Object nodeType;
	GenNode(int length) {
		node = new ANode(length);
		nodeType = ANODE;
	}
	GenNode(GenNode anode) {
		node = new FNode(anode);
		nodeType = FNODE;
	}
	GenNode(int hash, long key, Object value, GenNode type) {
		node = new SNode(hash, key, value, type);
		nodeType = SNODE;
	}
	GenNode(GenNode prev, int ppos, GenNode curr, int hash, int level) {
		node = new ENode(prev, ppos, curr, hash, level);
		nodeType = ENODE;
	}

}