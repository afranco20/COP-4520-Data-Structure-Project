import java.security.Key;
import java.util.concurrent.atomic.*;
public class ANode {
	AtomicReferenceArray<Object> array;
	//Object nodeType = ANODE;
	ANode (int length) {
		array = new AtomicReferenceArray<Object>(length);
	}
}