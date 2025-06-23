package org.bartekkansy.authguard.event;

import ch.njol.skript.Skript;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

public class SkriptEventDispatcher {

    private static boolean skriptAvailable = false;

    public static void init() {
        Plugin skript = Bukkit.getPluginManager().getPlugin("Skript");
        skriptAvailable = skript != null && skript.isEnabled();
        if (!skriptAvailable) {
            Bukkit.getLogger().warning("Skript plugin not found or not enabled — Skript events will not be dispatched.");
        }
    }

    public static void fireSkriptEvent(Event event) {
        if (skriptAvailable) {
            try {
                Bukkit.getPluginManager().callEvent(event);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to dispatch Skript event: " + e.getMessage());
            }
        }
    }
}