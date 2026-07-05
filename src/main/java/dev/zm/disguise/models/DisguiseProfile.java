package dev.zm.disguise.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DisguiseProfile {
    private final UUID playerUuid;
    private String name;
    private String skin;
    private String prefix;
    private String suffix;
    private String primaryGroup;
    private List<String> groups = new ArrayList<>();
    private long disguisedAt = System.currentTimeMillis();

    public DisguiseProfile(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSkin() {
        return skin;
    }

    public void setSkin(String skin) {
        this.skin = skin;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getPrimaryGroup() {
        return primaryGroup;
    }

    public void setPrimaryGroup(String primaryGroup) {
        this.primaryGroup = primaryGroup;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public void addGroup(String group) {
        if (!groups.contains(group)) {
            groups.add(group);
        }
    }

    public long getDisguisedAt() {
        return disguisedAt;
    }

    public void setDisguisedAt(long disguisedAt) {
        this.disguisedAt = disguisedAt;
    }
}
