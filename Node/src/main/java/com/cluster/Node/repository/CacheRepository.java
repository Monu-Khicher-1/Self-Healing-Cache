package com.cluster.Node.repository;


import com.cluster.Node.model.cache.CacheEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Repository
public class CacheRepository {
    private HashMap<String, CacheEntry> memoryMap;
    CacheRepository() {
        memoryMap = new HashMap<>();
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


    public HashMap<String, CacheEntry> getMemoryMap() {
        return memoryMap;
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

}
