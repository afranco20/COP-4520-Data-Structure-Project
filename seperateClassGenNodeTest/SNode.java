
import java.util.concurrent.atomic.*;
public class SNode {
	int hash;
	long key;
	Object value;
	AtomicReference<GenNode> txn;
	//Object nodeType = SNODE;

	SNode (int h, long k, Object val, GenNode type) {
		hash = h;
		key = k;
		value = val;
		txn = new AtomicReference<GenNode>(type);
	}
}