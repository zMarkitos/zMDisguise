package dev.zm.disguise.storage;

import dev.zm.disguise.models.DisguiseProfile;

import java.util.UUID;

public interface StorageService {
    void initialize();

    void save(UUID playerUuid, DisguiseProfile profile);

    DisguiseProfile load(UUID playerUuid);

    void delete(UUID playerUuid);

    void saveSavedProfile(UUID playerUuid, String saveName, DisguiseProfile profile);

    java.util.Map<String, DisguiseProfile> getSavedProfiles(UUID playerUuid);

    void deleteSavedProfile(UUID playerUuid, String saveName);

    void close();
}
