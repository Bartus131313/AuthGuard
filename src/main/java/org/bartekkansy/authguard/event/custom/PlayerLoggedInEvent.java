package org.bartekkansy.authguard.event.custom;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerLoginSuccessEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;

    public PlayerLoginSuccessEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}