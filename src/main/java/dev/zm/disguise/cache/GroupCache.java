package dev.zm.disguise.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroupCache {
    private final Map<String, String> cachedGroups = new ConcurrentHashMap<>();

    public String getGroup(String name) {
        return cachedGroups.get(name);
    }

    public void cacheGroup(String name, String groupName) {
        cachedGroups.put(name, groupName);
    }

    public void clear() {
        cachedGroups.clear();
    }
}
