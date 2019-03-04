import java.security.Key;
import java.util.concurrent.atomic.*;
public class FNode {
	ANode frozen;
	//Object nodeType = FNODE;
	FNode(ANode node) {
		frozen = node;
	}
}