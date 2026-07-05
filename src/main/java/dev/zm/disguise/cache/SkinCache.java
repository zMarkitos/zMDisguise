package dev.zm.disguise.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkinCache {
    private final Map<String, String> cachedSkins = new ConcurrentHashMap<>();

    public String getSkin(String name) {
        return cachedSkins.get(name);
    }

    public void cacheSkin(String name, String skinData) {
        cachedSkins.put(name, skinData);
    }

    public void clear() {
        cachedSkins.clear();
    }
}
