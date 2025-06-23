package org.bartekkansy.simplelogin.database;

import org.bartekkansy.simplelogin.AuthGuard;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private Connection connection;

    private final String host;
    private final int port;
    private final String databaseName;
    private final String username;
    private final String password;

    public DatabaseManager(AuthGuard plugin) {
        FileConfiguration config = plugin.getConfig();

        this.host = config.getString("database.host");
        this.port = config.getInt("database.port");
        this.databaseName = config.getString("database.name");
        this.username = config.getString("database.username");
        this.password = config.getString("database.password");
    }

    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) return;

        if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "0.0.0.0".equals(host)) {
            // Use SQLite local file database
            String filePath = String.format("plugins/SimpleLogin/%s.db", this.databaseName); // adjust path as needed
            String url = "jdbc:sqlite:" + filePath;
            connection = DriverManager.getConnection(url);
        } else {
            // Use MySQL connection
            String url = "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?useSSL=false&serverTimezone=UTC";
            connection = DriverManager.getConnection(url, username, password);
        }

        createTableIfNotExists();
    }

    private void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS players (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "uuid VARCHAR(36) NOT NULL UNIQUE," +
                "password_hash VARCHAR(64) NOT NULL" +
                ")";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    public void savePlayerPassword(String uuid, String plainPassword) throws SQLException {
        String passwordHash = hashPassword(plainPassword);

        String sql = "INSERT INTO players (uuid, password_hash) VALUES (?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET password_hash = excluded.password_hash";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setString(2, passwordHash);
            stmt.executeUpdate();
        }
    }

    public boolean isPlayerRegistered(String uuid) throws SQLException {
        String sql = "SELECT 1 FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // if result exists, player is registered
            }
        }
    }

    public List<Player> getRegisteredPlayers() throws SQLException {
        List<Player> registeredPlayers = new ArrayList<>();

        String sql = "SELECT uuid FROM players";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                registeredPlayers.add(Bukkit.getPlayer(rs.getString("uuid")));
            }
        }

        return registeredPlayers;
    }

    public boolean removePlayer(String uuid) throws SQLException {
        String sql = "DELETE FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                System.out.println("No player found with UUID: " + uuid);
                return false;
            }
        }
        return true;
    }

    public String getPlayerPasswordHash(String uuid) throws SQLException {
        String sql = "SELECT password_hash FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
                return null;
            }
        }
    }

    public boolean isPasswordCorrect(String uuid, String password) throws SQLException {
        String storedHash = getPlayerPasswordHash(uuid);
        if (storedHash == null) {
            return false;
        }
        String inputHash = hashPassword(password);
        return storedHash.equals(inputHash);
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
        }
    }

    public boolean isConnected() throws SQLException {
        return connection != null && !connection.isClosed();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found!", e);
        }
    }
}