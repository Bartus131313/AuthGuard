package org.bartekkansy.authguard.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public class LangManager {

    private FileConfiguration langConfig;

    private final JavaPlugin plugin;

    public LangManager(JavaPlugin plugin) {
        this.plugin = plugin;

        reload();
    }

    public void reload() {
        FileConfiguration config = this.plugin.getConfig();
        loadLangFile(String.format("translations/lang_%s.yml", config.getString("main.language")));
    }

    public void loadLangFile(String fileName) {
        File langFile = new File(plugin.getDataFolder(), fileName);
        if (!langFile.exists()) {
            plugin.saveResource(fileName, false); // copy from jar to plugin folder
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public String getString(String key) {
        return langConfig.getString(key, "$# Missing translation for: " + key);
    }

    // Optional: get string with placeholders replaced
    public String getString(String key, Map<String, String> placeholders) {
        String message = getString(key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return message;
    }
}