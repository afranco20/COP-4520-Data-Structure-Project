import java.security.Key;
import java.util.concurrent.atomic.*;
public class SNode {
	int hash;
	Key key;
	Object value;
	AtomicReference<Object> txn;
	//Object nodeType = SNODE;

	SNode (int h, Key k, Object val, Object type) {
		hash = h;
		key = k;
		value = val;
		txn = new AtomicReference<Object>(type);
	}
}