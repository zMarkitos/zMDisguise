package dev.zm.disguise.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileCache {
    // Basic cache structure
    private final Map<String, Object> cachedProfiles = new ConcurrentHashMap<>();

    public Object getProfile(String name) {
        return cachedProfiles.get(name);
    }

    public void cacheProfile(String name, Object profile) {
        cachedProfiles.put(name, profile);
    }

    public void clear() {
        cachedProfiles.clear();
    }
}
