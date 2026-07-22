package com.cluster.Master.utils.hashing;

import com.cluster.Master.utils.HashingStrategy;

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
