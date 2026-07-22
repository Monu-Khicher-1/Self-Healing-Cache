package com.cluster.Master.utils;

import com.cluster.Master.utils.hashing.SimpleHashing;

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
