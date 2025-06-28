package org.bartekkansy.authguard.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bartekkansy.authguard.AuthGuard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthGuardPlaceholder extends PlaceholderExpansion {
    private JavaPlugin plugin;

    public AuthGuardPlaceholder(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        // This expansion will stay loaded even if PlaceholderAPI reloads
        return true;
    }

    @Override
    public String getIdentifier() { return "authguard"; }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    // This is where you handle placeholders
    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params.equalsIgnoreCase("logged_in")) {
            return String.valueOf(AuthGuard.isPlayerLoggedIn(player));
        }

        if (params.equalsIgnoreCase("registered")) {
            return String.valueOf(AuthGuard.isPlayerRegistered(player));
        }

        return null;
    }
}