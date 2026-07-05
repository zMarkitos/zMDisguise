package dev.zm.disguise.hooks;

public class SkinHook {
    private boolean enabled;

    public void register() {
        enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSkin(String playerName) {
        return null;
    }
}
