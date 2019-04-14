
import java.util.concurrent.atomic.AtomicReference;

public class CacheTrie extends CTrieNoCache{
    AtomicReference<Cache> cacheHead = new AtomicReference<>(null);
    void inhabit(Cache cache, GenNode val, int hash, int currLevels) {
        if(cache == null) {
            if(currLevels >= 12) {
                cache = new Cache(8, null);
                cacheHead.compareAndSet(null, cache);
                Cache newCache = cacheHead.get();
                inhabit(newCache, val, hash, currLevels);
            }
        }
        else {
            int length = cache.root.length() ;
            int cacheLevel = Integer.numberOfTrailingZeros(length - 1);
            if(currLevels == cacheLevel) {
                //????? remove "1 ="
                int pos = 1 + (hash & (length - 2));
                cache.root.set(pos, val);
            }
        }
    }

    void recordCacheMiss() {
        //System.out.println("Invoke Cache miss");
    }

    boolean completeExpansion(GenNode en, Cache curr) {
        if(completeExpansion(en)) {
            inhabit(curr, ((ENode)en.node).wide.get(), ((ENode)en.node).hash, ((ENode)en.node).level);
            return true;
        }
        return false;
    }

    @Override
    Object lookup(Object key) {
        Cache currCache = (cacheHead == null) ? null : cacheHead.get();
        SNode result = lookup(key, hash(key.hashCode()), 0, root, currCache);
        if(result != null) {
            return result.value;
        }
        else {
            return null;
        }
    }

    SNode lookup(Object key, int hash, int level, GenNode curr, Cache currCache) {

        if(currCache != null && ((1 << level) == (currCache.root.length() - 1))) {
            inhabit(currCache, curr, hash, level);
        }

        int pos = ((hash >>> level) & ((((ANode)curr.node).array).length() - 1));
        GenNode old = ((ANode)curr.node).array.get(pos);

        if (old == null) {
            return null;
        }

        switch (old.nodeType) {
            // Not sure about "old == FVNODE"
            case FNODE:
                if (((FNode) old.node).frozen == null) {
                    return null;
                }

                switch ((String) ((FNode) old.node).AorS) {
                    case ANODE:
                        return lookup(key, hash, level + 4, ((FNode) old.node).frozen, currCache);

                    case SNODE:
                        GenNode fnode = ((FNode) old.node).frozen;
                        return (((SNode) fnode.node).key == key) ? (SNode) fnode.node : null;

                    default:
                        return null;
                }

            case ANODE:
                return lookup(key, hash, level + 4, old, currCache);

            case SNODE:
                int cacheLevel = (currCache == null) ? 0 : 31 - Integer.numberOfTrailingZeros(currCache.root.length() - 1);
                if(level < cacheLevel || level >= cacheLevel + 8) {
                    recordCacheMiss();
                }
                if(currCache != null && ((1 << (level + 4)) == (currCache.root.length() - 1))) {
                    inhabit(currCache, curr, hash, level + 4);
                }
                return (((SNode) old.node).key == key) ? (SNode) old.node : null;

            case ENODE:
                GenNode an = ((ENode) old.node).narrow;
                return lookup(key, hash, level + 4, an, currCache);

            default:
                System.out.println("### ERROR IN LOOKUP! ###");
                return null;
        }
    }

    Object fastLookup(Object key) {
        int hash = hash(key.hashCode());
        SNode result = fastLookup(key, hash);
        if(result != null) {
            return result.value;
        }
        else {
            return null;
        }
    }

    SNode fastLookup(Object key, int hash) {

        Cache currCache = (cacheHead == null) ? null : cacheHead.get();

        if(currCache == null) {
            return lookup(key, hash, 0, root, currCache);
        }
        int sizeCache = currCache.root.length();
        //int topLevel = 31 - Integer.numberOfTrailingZeros(sizeCache - 1);
        int pos = 1 + (hash & (sizeCache - 2));
        GenNode curr = currCache.root.get(pos);
        int currLevel = 31 - Integer.numberOfTrailingZeros(sizeCache - 1);
        if(curr == null) {
            return lookup(key, hash, 0, root, currCache);
        }
        else if(curr.nodeType.equals(SNODE)) {
            GenNode txn = ((SNode)curr.node).txn.get();
            if(txn == null) {
                if(((SNode) curr.node).key == key) {
                    return ((SNode) curr.node);
                }
                else {
                    return null;
                }
            }
            else {
                return lookup(key, hash, 0, root, currCache);
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
                        return ((SNode) old.node);
                    }
                    else {
                        return null;
                    }
                }
                else {
                    return lookup(key, hash, 0, root, currCache);
                }
            }
            else if(old.nodeType.equals(ENODE)) {
                completeExpansion(old, currCache);
                return fastLookup(key, hash);
            }
        }
        else {
            System.out.println("Fast lookup error");
            return null;
        }
        return lookup(key, hash, 0, root, currCache);
    }
    @Override
    void insert(Object key, Object val) {
        int hash = hash(key.hashCode());
        insert(key, val, hash);
    }

    void insert(Object key, Object val, int hash) {
        Cache currCache = (cacheHead == null)? null: cacheHead.get();
        if(!insert(key, val, hash, 0, root, null, currCache)) {
            insert(key, val, hash);
        }
    }

    boolean insert(Object k, Object v, int h, int lev, GenNode curr, GenNode prev, Cache currCache) {

        if(currCache != null && ((1 << lev) == (currCache.root.length() - 1))) {
            inhabit(currCache, curr, h, lev);
        }

        // Get position in ANode according to size of ANode, level and hash code
        int pos = (h >>> lev) & (((ANode) curr.node).array.length() - 1);

        // Check what is at the current position
        GenNode old = ((ANode) curr.node).array.get(pos);



        ////////////////////////////////////////////////////////////////////////////////////////////////////
        // DEBUG
        ////////////////////////////////////////////////////////////////////////////////////////////////////

//    System.out.printf("--- insert ---%n");
//    System.out.printf("level: %d%n%n", lev);
//    System.out.printf("array values:%n");
//    for(int i = 0; i < ((ANode) curr.node).array.length(); i++) {
//      if(((ANode) curr.node).array.get(i) != null)
//        System.out.println("[" + i + "] = " + ((ANode) curr.node).array.get(i).nodeType);
//      else
//        System.out.println("[" + i + "] = " + "null");
//    }

        ////////////////////////////////////////////////////////////////////////////////////////////////////


        // If current position is empty
        if (old == null) {

            int cacheLevel = (currCache == null) ? 0 : 31 - Integer.numberOfTrailingZeros(currCache.root.length() - 1);

            if(lev < cacheLevel || lev >= cacheLevel + 8) {
                recordCacheMiss();
            }

            //System.out.println("Congrats");
            GenNode sn = new GenNode(h, k, v, null);

            // Check nothing has changed, then insert
            if (((ANode) curr.node).array.compareAndSet(pos, null, sn)) {
                return true;
            }

            // Collision, restart at next level
            else {
                return insert(k, v, h, lev, curr, prev, currCache);
            }
        }

        // If there is another array at position, jump to that array
        else if (old.nodeType.equals(ANODE)) {
            //System.out.println("Heyooo");
            return insert(k, v, h, lev + 4, old, curr, currCache);
        }

        // If we found an entry with the current hash bits
        else if (old.nodeType.equals(SNODE)) {
            // Check if it has a special value
            GenNode txn = ((SNode) old.node).txn.get();

            //System.out.println("what");
            // We're allowed to modify the node
            if (txn == null) {
                //System.out.println("whater");
                // If we want to insert a node with the same key
                if (((SNode) old.node).key == k) {
                    GenNode sn = new GenNode(h, k, v, null);

                    // Check if nothing has changed and announce that there is new SNode
                    if (((SNode) old.node).txn.compareAndSet(null, sn)) {
                        ((ANode) curr.node).array.compareAndSet(pos, old, sn);
                        return true;
                    }

                    // Failed to announce there is new SNode so restart
                    else {
                        return insert(k, v, h, lev, curr, prev, currCache);
                    }
                }

                // Check if ANode is narrow, create ENode to announce expansion
                else if (((ANode) curr.node).array.length() == 4) {
                    // Save the position in the parent
                    int ppos = (h >>> (lev - 4)) & (((ANode) prev.node).array.length() - 1);
                    //System.out.println("length 4");
                    GenNode en = new GenNode(prev, ppos, curr, h, lev);

                    // Check that the parent of the current ANode still contains current and then insert the ENode
                    if (((ANode) prev.node).array.compareAndSet(ppos, curr, en)) {
                        completeExpansion(en, currCache);
                        GenNode wide = ((ANode) prev.node).array.get(ppos);

                        // Restart insertion with the wide ANode in the new ENode
                        return insert(k, v, h, lev, wide, prev, currCache);
                    }

                    // Failed to insert ENode so restart
                    else {
                        return insert(k, v, h, lev, curr, prev, currCache);
                    }
                }

                // If ANode is already wide
                else {
                    GenNode sn = new GenNode(h, k, v, null);

                    // Creates a new ANode with the new and old SNode
                    GenNode an = createNarrowOrWide(old, sn, lev + 4);

                    // Check if nothing has changed, replace old ANode with new ANode
                    if (((SNode) old.node).txn.compareAndSet(null, an)) {
                        ((ANode) curr.node).array.compareAndSet(pos, old, an);

                        // ??? evil bit level magic [don't remove]
                        ((SNode) old.node).txn.compareAndSet(an, null);
                        //System.out.println("new narrow");
                        return true;
                    }

                    // Failed so restart
                    else {
                        return insert(k, v, h, lev, curr, prev, currCache);
                    }
                }
            }


            // Frozen SNode so unable to make changes, returns back up to the ENode in control of this SNode
            else if (txn.nodeType.equals(FNODE) && ((FNode)txn.node).frozen.nodeType.equals(SNODE)) {
                return false;
            }

            // If SNode or ANode, help complete concurrent insertion
            else {
                // Help update cur[pos] to value at txn
                //System.out.println("hey");
                ((ANode) curr.node).array.compareAndSet(pos, old, txn);
                return insert(k, v, h, lev, curr, prev, currCache);
            }
        }

        // Help concurrent expansion
        else if (old.nodeType.equals(ENODE)) {
            completeExpansion(old, currCache);
        }

        // If FVNode or FNode
        return false;
    }

    void fastInsert(Object key, Object val) {
        Cache currCache = (cacheHead == null) ? null : cacheHead.get();
        fastInsert(key, val, hash(key.hashCode()), currCache, currCache);
    }

    void fastInsert(Object key, Object value, int hash, Cache curr, Cache prev) {

        if (curr == null) {
            insert(key, value, hash);
        } else {
            int pos = 1 + (hash & (curr.root.length() - 2));
            GenNode node = curr.root.get(pos);
            int level = 31 - Integer.numberOfLeadingZeros(curr.root.length() - 1);
            if (node == null) {
                CacheNode stats = curr.stats.get();
                Cache parent = stats.parent.get();
                fastInsert(key, value, hash, parent, curr);
            } else if (node.nodeType.equals(ANODE)) {
                int aPos = (hash >>> level) & (((ANode) node.node).array.length() - 1);
                GenNode old = ((ANode) node.node).array.get(aPos);
                if (old == null) {
                    GenNode sn = new GenNode(hash, key, value, null);
                    if (((ANode) node.node).array.compareAndSet(aPos, old, sn)) {
                        return;
                    } else {
                        fastInsert(key, value, hash, curr, prev);
                    }
                } else if (old.nodeType.equals(ANODE)) {
                    if (!insert(key, value, hash, level + 4, old, node, prev)) {
                        fastInsert(key, value, hash, curr, prev);
                    }
                } else if (old.nodeType.equals(SNODE)) {
                    GenNode txn = ((SNode) old.node).txn.get();
                    if (txn == null) {
                        if (((SNode) old.node).key == key) {
                            GenNode sn = new GenNode(hash, key, value, null);
                            if (((SNode) old.node).txn.compareAndSet(null, sn)) {
                                ((ANode) node.node).array.compareAndSet(aPos, old, sn);
                            } else {
                                fastInsert(key, value, hash, curr, prev);
                            }
                        } else if (((ANode) node.node).array.length() == 4) {
                            CacheNode stats = curr.stats.get();
                            Cache parent = stats.parent.get();
                            fastInsert(key, value, hash, parent, curr);
                        } else {
                            GenNode sn = new GenNode(hash, key, value, null);
                            GenNode newANode = createNarrowOrWide(old, sn, level + 4);
                            if (((SNode) old.node).txn.compareAndSet(txn, newANode)) {
                                ((ANode) node.node).array.compareAndSet(aPos, old, newANode);

                                //??????
                                ((SNode) old.node).txn.compareAndSet(newANode, null);
                            } else {
                                fastInsert(key, value, hash, curr, prev);
                            }
                        }
                    } else if (txn.nodeType.equals(FNODE) && ((FNode) txn.node).AorS.equals(SNODE)) {
                        insert(key, value, hash);
                    } else {
                        ((ANode) node.node).array.compareAndSet(aPos, old, txn);
                        fastInsert(key, value, hash, curr, prev);
                    }
                } else {
                    insert(key, value, hash);
                }
            } else if (node.nodeType.equals(SNODE)) {
                CacheNode stats = curr.stats.get();
                Cache parent = stats.parent.get();
                fastInsert(key, value, hash, parent, curr);
            } else {
                System.out.println("Error fastInsert");
            }
        }
    }


    void printCache() {
        Cache temp = cacheHead.get();
        CacheNode stats;
        int cacheLevel = 0;
        while(temp != null){
            System.out.printf("cache %d%n", cacheLevel++);
            stats = temp.stats.get();
            int length = temp.root.length();
            for (int i = 0; i < length; i++) {
                GenNode item = temp.root.get(i);
                if(item == null) {
                    System.out.println("null");
                    continue;
                }
                switch (item.nodeType) {
                    case SNODE:
                        System.out.println(((SNode) item.node).value);
                        break;
                    case ANODE:
                        printTrace(item, 0);
                        break;
                    case ENODE:
                        printTrace(((ENode) item.node).narrow, 0); // narrow
                        break;
                    case FNODE:
                        if (((FNode) item.node).AorS.equals(ANODE)) {
                            printTrace((GenNode) ((FNode) item.node).frozen.node, 0);
                        } else if (((FNode) item.node).AorS.equals(SNODE)) {
                            System.out.println(((SNode) ((FNode) item.node).frozen.node).value);
                        }
                        break;
                    default:
                        System.out.println("null");
                }
            }
            temp = stats.parent.get();
        }
    }
}
