
import java.util.concurrent.atomic.AtomicReference;

public class CacheTrie extends CTrieNoCache{
    AtomicReference<Cache> cacheHead = null;
    void inhabit(GenNode val, int hash, int currLevels) {
        if(cacheHead.get() == null) {
            if(currLevels >= 12) {
                cacheHead.get().createCacheLev(8);
                //cacheHead.compareAndSet(null, curr);
                inhabit(val, hash, currLevels);
            }
        }
        else {
            int length = cacheHead.get().root.length() ;
            int cacheLevel = Integer.numberOfTrailingZeros(length - 1);
            if(currLevels == cacheLevel) {
                //????? remove "1 ="
                int pos = (hash & (length - 2));
                cacheHead.get().root.set(pos, val);
            }
        }
    }

    void recordCacheMiss() {
        System.out.println("Invoke Cache miss");
    }

    @Override
    boolean completeExpansion(GenNode en) {
        if(this.completeExpansion(en)) {
            inhabit(((ENode)en.node).wide.get(), ((ENode)en.node).hash, ((ENode)en.node).level);
            return true;
        }
        return false;
    }

    @Override
    Object lookup(Object k) {
        SNode result = lookup(k, hash(k.hashCode()), 0, root, 0);
        if(result != null) {
            return result.value;
        }
        else {
            return null;
        }
    }

    SNode lookup(Object key, int hash, int level, GenNode curr, int cacheLevel) {

        if(level == cacheLevel)
            inhabit(curr, hash, level);

        int pos = ((hash >>> level) & ((((ANode)curr.node).array).length() - 1));
        GenNode old = ((ANode)curr.node).array.get(pos);
        if(old == null)
            return lookup(key, hash, level, curr);
        else if (old.nodeType.equals(SNODE)) {
            if(level < cacheLevel || level > cacheLevel + 4) {
              recordCacheMiss();
            }
            if(level + 4 == cacheLevel) {
                inhabit(old, hash, level + 4);
            }
        }
        return lookup(key, hash, level, curr);
    }

    Object fastLookup(Object key, int hash) {
        if(cacheHead == null) {
            return lookup(key, hash, 0, root, -1);
        }
        int sizeCache = cacheHead.get().root.length();
        int topLevel = Integer.numberOfTrailingZeros(sizeCache - 1);
        int pos = (hash & (sizeCache - 2));
        GenNode curr = cacheHead.get().root.get(pos);
        int currLevel = Integer.numberOfTrailingZeros(sizeCache - 1);
        if(curr.nodeType.equals(SNODE)) {
            GenNode txn = ((SNode)curr.node).txn.get();
            if(txn == null) {
                if(((SNode) curr.node).key == key) {
                    return ((SNode) curr.node).value;
                }
                else {
                    return null;
                }
            }
            else {
                return lookup(key, hash, 0, root, currLevel).value;
            }
        }
        else if(curr.nodeType.equals(ANODE)) {
            int cpos = (hash >>> currLevel) & (((ANode)curr.node).array.length() - 1);
            GenNode old = ((ANode) curr.node).array.get(cpos);
            if(old == null) {
                return null;
            }
            else if(old.nodeType.equals(SNODE)) {
                GenNode txn = ((SNode) old.node).txn.get();
                if(txn == null) {
                    if(((SNode) old.node).key == key) {
                        return ((SNode) old.node).value;
                    }
                    else {
                        return null;
                    }
                }
                else {
                    return lookup(key, hash, 0, root, currLevel).value;
                }
            }
            else if(old.nodeType.equals(ENODE)) {
                completeExpansion(old);
                fastLookup(key, hash);
            }
        }
        else {
            System.out.println("Fast lookup error");
            return null;
        }
        return lookup(key, hash, 0, root, topLevel);
    }
}
