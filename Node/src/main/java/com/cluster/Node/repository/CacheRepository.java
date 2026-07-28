package com.cluster.Node.repository;


import com.cluster.Node.model.cache.CacheEntry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Slf4j
@Repository
public class CacheRepository {
    private final Map<String, CacheEntry> memoryMap;
    CacheRepository() {
        memoryMap = new ConcurrentHashMap<>();
    }

    public CacheEntry get(String key){
        return memoryMap.get(key);
    }

    public CacheEntry put(String key, CacheEntry entry){
        memoryMap.put(key,entry);
        return entry;
    }

    public void removeValue(CacheEntry entry){
        memoryMap.remove(entry.getKey());
    }

    public void removeByKey(String key){
        memoryMap.remove(key);
    }


    public List<CacheEntry> getAllValues(){
        log.info("Getting cache list from repository");
        return new ArrayList<>(memoryMap.values());
    }

    public List<CacheEntry> getEntriesByHashRange(long startHash, long endHash) {
        log.info("Getting cache entries between hash range [{}, {}]", startHash, endHash);
        List<CacheEntry> entries = new ArrayList<>();
        for (CacheEntry entry : memoryMap.values()) {
            long keyHash = entry.getKey().hashCode();
            log.info("Cacheentry: {}, Corresponding Hash: {}", entry.getKey(), keyHash);
            // Check if key hash falls in the range (startHash, endHash]
            if(startHash <= endHash){
                if (keyHash > startHash && keyHash <= endHash) entries.add(entry);
            }else{
                if (keyHash > startHash || keyHash <= endHash) entries.add(entry);
            }
        }
        log.info("Found {} entries in hash range", entries.size());
        return entries;
    }

    /** Number of entries whose key hash falls in {@code (startHash, endHash]} (wrap-aware). */
    public long countByHashRange(long startHash, long endHash) {
        return getEntriesByHashRange(startHash, endHash).size();
    }

    /**
     * Drops every entry whose key hash falls in {@code (startHash, endHash]} (wrap-aware). Used to
     * safely remove obsolete copies from an old owner after a replica-set transition completes.
     *
     * @return number of entries removed
     */
    public long removeByHashRange(long startHash, long endHash) {
        List<CacheEntry> toRemove = getEntriesByHashRange(startHash, endHash);
        for (CacheEntry entry : toRemove) {
            memoryMap.remove(entry.getKey());
        }
        log.info("Removed {} entries in hash range [{}, {}]", toRemove.size(), startHash, endHash);
        return toRemove.size();
    }

}
