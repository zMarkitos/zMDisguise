package dev.zm.disguise.storage;

import dev.zm.disguise.zMDisguise;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteStorage implements StorageService {

    private Connection connection;

    @Override
    public void initialize() {
        File dataFolder = zMDisguise.getInstance().getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dbFile = new File(dataFolder, "disguises.db");
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS disguises (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "name VARCHAR(16), " +
                        "skin TEXT, " +
                        "prefix TEXT, " +
                        "suffix TEXT, " +
                        "group_name TEXT" +
                        ")");
                statement.execute("CREATE TABLE IF NOT EXISTS saved_disguises (" +
                        "uuid VARCHAR(36), " +
                        "save_name VARCHAR(32), " +
                        "name VARCHAR(16), " +
                        "skin TEXT, " +
                        "prefix TEXT, " +
                        "suffix TEXT, " +
                        "group_name TEXT, " +
                        "PRIMARY KEY (uuid, save_name)" +
                        ")");
            }
        } catch (SQLException | ClassNotFoundException e) {
            zMDisguise.getInstance().getLogger().severe("Failed to initialize SQLite: " + e.getMessage());
        }
    }

    @Override
    public void save(java.util.UUID playerUuid, dev.zm.disguise.models.DisguiseProfile profile) {
        if (connection == null)
            return;
        try (java.sql.PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO disguises (uuid, name, skin, prefix, suffix, group_name) VALUES (?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, profile.getName());
            stmt.setString(3, profile.getSkin());
            stmt.setString(4, profile.getPrefix());
            stmt.setString(5, profile.getSuffix());

            // Serialize groups into a single string separated by commas
            String groupStr = profile.getPrimaryGroup() + ";" + String.join(",", profile.getGroups());
            stmt.setString(6, groupStr);
            stmt.executeUpdate();
        } catch (SQLException e) {
            zMDisguise.getInstance().getLogger().severe("Failed to save disguise: " + e.getMessage());
        }
    }

    @Override
    public dev.zm.disguise.models.DisguiseProfile load(java.util.UUID playerUuid) {
        if (connection == null)
            return null;
        try (java.sql.PreparedStatement stmt = connection.prepareStatement("SELECT * FROM disguises WHERE uuid = ?")) {
            stmt.setString(1, playerUuid.toString());
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    dev.zm.disguise.models.DisguiseProfile profile = new dev.zm.disguise.models.DisguiseProfile(
                            playerUuid);
                    profile.setName(rs.getString("name"));
                    profile.setSkin(rs.getString("skin"));
                    profile.setPrefix(rs.getString("prefix"));
                    profile.setSuffix(rs.getString("suffix"));

                    String groupStr = rs.getString("group_name");
                    if (groupStr != null) {
                        if (groupStr.contains(";")) {
                            String[] parts = groupStr.split(";", 2);
                            profile.setPrimaryGroup(parts[0]);
                            if (parts.length > 1 && !parts[1].isEmpty()) {
                                profile.setGroups(
                                        new java.util.ArrayList<>(java.util.Arrays.asList(parts[1].split(","))));
                            }
                        } else {
                            profile.setPrimaryGroup(groupStr);
                            profile.addGroup(groupStr);
                        }
                    }

                    return profile;
                }
            }
        } catch (SQLException e) {
            zMDisguise.getInstance().getLogger().severe("Failed to load disguise: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void delete(java.util.UUID playerUuid) {
        if (connection == null)
            return;
        try (java.sql.PreparedStatement stmt = connection.prepareStatement("DELETE FROM disguises WHERE uuid = ?")) {
            stmt.setString(1, playerUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            zMDisguise.getInstance().getLogger().severe("Failed to delete disguise: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void saveSavedProfile(java.util.UUID playerUuid, String saveName,
            dev.zm.disguise.models.DisguiseProfile profile) {
        if (connection == null)
            return;
        try (java.sql.PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO saved_disguises (uuid, save_name, name, skin, prefix, suffix, group_name) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, saveName.toLowerCase());
            stmt.setString(3, profile.getName());
            stmt.setString(4, profile.getSkin());
            stmt.setString(5, profile.getPrefix());
            stmt.setString(6, profile.getSuffix());

            String groupStr = profile.getPrimaryGroup() + ";" + String.join(",", profile.getGroups());
            stmt.setString(7, groupStr);
            stmt.executeUpdate();
        } catch (SQLException e) {
            zMDisguise.getInstance().getLogger().severe("Failed to save saved profile: " + e.getMessage());
        }
    }

    @Override
    public java.util.Map<String, dev.zm.disguise.models.DisguiseProfile> getSavedProfiles(java.util.UUID playerUuid) {
        java.util.Map<String, dev.zm.disguise.models.DisguiseProfile> map = new java.util.HashMap<>();
        if (connection == null)
            return map;
        try (java.sql.PreparedStatement stmt = connection
                .prepareStatement("SELECT * FROM saved_disguises WHERE uuid = ?")) {
            stmt.setString(1, playerUuid.toString());
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    dev.zm.disguise.models.DisguiseProfile profile = new dev.zm.disguise.models.DisguiseProfile(
                            playerUuid);
                    profile.setName(rs.getString("name"));
                    profile.setSkin(rs.getString("skin"));
                    profile.setPrefix(rs.getString("prefix"));
                    profile.setSuffix(rs.getString("suffix"));

                    String groupStr = rs.getString("group_name");
                    if (groupStr != null) {
                        if (groupStr.contains(";")) {
                            String[] parts = groupStr.split(";", 2);
                            profile.setPrimaryGroup(parts[0]);
                            if (parts.length > 1 && !parts[1].isEmpty()) {
                                profile.setGroups(
                                        new java.util.ArrayList<>(java.util.Arrays.asList(parts[1].split(","))));
                            }
                        } else {
                            profile.setPrimaryGroup(groupStr);
                            profile.addGroup(groupStr);
                        }
                    }

                    map.put(rs.getString("save_name"), profile);
                }
            }
        } catch (SQLException e) {
            zMDisguise.getInstance().getLogger().severe("Failed to load saved profiles: " + e.getMessage());
        }
        return map;
    }

    @Override
    public void deleteSavedProfile(java.util.UUID playerUuid, String saveName) {
        if (connection == null)
            return;
        try (java.sql.PreparedStatement stmt = connection
                .prepareStatement("DELETE FROM saved_disguises WHERE uuid = ? AND save_name = ?")) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, saveName.toLowerCase());
            stmt.executeUpdate();
        } catch (SQLException e) {
            zMDisguise.getInstance().getLogger().severe("Failed to delete saved profile: " + e.getMessage());
        }
    }
}
