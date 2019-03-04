import java.security.Key;
import java.util.concurrent.atomic.*;

// Not sure about making parent and wide atomic, if so might need narrow atomic as well
public class ENode {
	AtomicReference<ANode> parent;
	int parentpos;
	ANode narrow;
	int hash;
	int level;
	AtomicReference<ANode> wide;
	//Object nodeType = ENODE;

	ENode(ANode prev, int ppos, ANode curr, int h, int lev) {
		parent = new AtomicReference<ANode>(prev);
		parentpos = ppos;
		narrow = curr;
		hash = h;
		level = lev;
	}
}