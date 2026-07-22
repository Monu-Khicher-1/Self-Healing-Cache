package com.cluster.Node.utils.hashing;

import com.cluster.Node.utils.HashingStrategy;

public class SimpleHashing extends HashingStrategy {
    @Override
    public long getNodeHash(String key) {
        return key.hashCode();
    }

    @Override
    public long getKeyHash(String key) {
        return key.hashCode();
    }
}
