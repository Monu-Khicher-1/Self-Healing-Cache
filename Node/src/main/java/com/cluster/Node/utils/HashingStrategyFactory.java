package com.cluster.Node.utils;

import com.cluster.Node.utils.hashing.SimpleHashing;

public class HashingStrategyFactory {
    private HashingStrategyFactory() {}
    public static HashingStrategy getHashingStrategy(String strategy) {
        switch (strategy) {
            case "SIMPLE":
                return new SimpleHashing();
            default:
                throw new IllegalArgumentException("Unknown hashing strategy: " + strategy);
        }
    }
}
