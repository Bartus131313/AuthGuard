package org.bartekkansy.authguard.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class Util {
    public static boolean isPluginEnabled(String name) {
        Plugin p = Bukkit.getPluginManager().getPlugin(name);
        return p != null && p.isEnabled();
    }
}
