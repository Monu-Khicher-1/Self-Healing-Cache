package com.cluster.Master.utils;


public abstract class HashingStrategy {
    public abstract long getNodeHash(String key);
    public abstract long getKeyHash(String key);
}
