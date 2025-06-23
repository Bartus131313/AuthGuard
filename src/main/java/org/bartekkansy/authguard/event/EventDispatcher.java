package org.bartekkansy.authguard.event;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;
import org.bartekkansy.authguard.event.custom.PlayerLoggedInEvent;
import org.bartekkansy.authguard.event.custom.PlayerRegisteredEvent;
import org.bartekkansy.authguard.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class EventDispatcher {
    public static void init() {
        if (!Util.isPluginEnabled("Skript")) {
            Bukkit.getLogger().warning("Skript plugin not found or not enabled — Skript events will not be dispatched.");
            return;
        }

        // Register Skript event - PlayerLoggedInEvent
        Skript.registerEvent("Player Logged In", SimpleEvent.class, PlayerLoggedInEvent.class, "player logged in");
        EventValues.registerEventValue(PlayerLoggedInEvent.class, Player.class, new Getter<>() {
            @Override
            public Player get(PlayerLoggedInEvent event) {
                return event.getPlayer();
            }
        }, 0);

        // Register Skript event - PlayerRegisteredEvent
        Skript.registerEvent("Player Registered", SimpleEvent.class, PlayerRegisteredEvent.class, "player registered");
        EventValues.registerEventValue(PlayerRegisteredEvent.class, Player.class, new Getter<>() {
            @Override
            public Player get(PlayerRegisteredEvent event) {
                return event.getPlayer();
            }
        }, 0);
    }

    public static void fireEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }
}