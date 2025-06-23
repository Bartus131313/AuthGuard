package org.bartekkansy.simplelogin.event;

import org.bartekkansy.simplelogin.managers.LangManager;
import org.bartekkansy.simplelogin.LoginState;
import org.bartekkansy.simplelogin.managers.MessageManager;
import org.bartekkansy.simplelogin.AuthGuard;
import org.bartekkansy.simplelogin.commands.RegisterCommand;
import org.bartekkansy.simplelogin.database.DatabaseManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.Map;

public class EventListener implements Listener {

    private AuthGuard plugin;

    private DatabaseManager databaseManager;
    private LangManager langManager;
    private MessageManager messageManager;

    public EventListener(AuthGuard plugin) {
        this.plugin = plugin;

        this.databaseManager = this.plugin.getDatabaseManager();
        this.langManager = this.plugin.getLangManager();
        this.messageManager = this.plugin.getMessageManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Set Vector.ZERO velocity
        player.setVelocity(new Vector());

        // Play sound on start
        player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.0f);

        // Teleport player on spawn
        String spawnLocation = this.plugin.getConfig().getString("main.spawn_location");
        if (spawnLocation.equalsIgnoreCase("WORLD_SPAWN")) player.teleport(player.getWorld().getSpawnLocation().add(0.5, 0, 0.5));

        try { // States: 0 - register | 1 - login
            if (this.databaseManager.isPlayerRegistered(player.getUniqueId().toString())) {
                player.setMetadata("login_state", new FixedMetadataValue(this.plugin, LoginState.LOGIN.name()));
            } else {
                player.setMetadata("login_state", new FixedMetadataValue(this.plugin, LoginState.REGISTER.name()));

                // Generate new captcha
                RegisterCommand.setNewCaptchaForPlayer(this.plugin, player);
            }
        } catch (SQLException e) {
            e.printStackTrace();

            player.kickPlayer(MessageManager.toLegacy(langManager.getString("kick_error", Map.of("error_code", "$SQL.onPlayerJoin()"))));
            return;
        }

        player.setMetadata("logged_in", new FixedMetadataValue(this.plugin, false));
        player.setMetadata("login_tries", new FixedMetadataValue(this.plugin, 0));

        // Display welcome message
        displayWelcomeMessage(player);

        kickPlayerAfterTime(player, this.plugin.getConfig().getInt("main.kick_timeout"));
    }

    private void displayWelcomeMessage(Player player) {
        int loginState = AuthGuard.getPlayerLoginState(player).ordinal();

        String key = loginState == 1 ? "welcome_login" : "welcome_register";
        messageManager.sendMessageToPlayer(player, key, Map.of("player", player.getDisplayName()));
    }

    // Cancel all event when player is not logged in
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!AuthGuard.isPlayerLoggedIn(player)) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (!AuthGuard.isPlayerLoggedIn(player)) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!AuthGuard.isPlayerLoggedIn(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!AuthGuard.isPlayerLoggedIn(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!AuthGuard.isPlayerLoggedIn(player)) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!AuthGuard.isPlayerLoggedIn(event.getPlayer())) event.setCancelled(true);
    }
    ///

    private void kickPlayerAfterTime(Player player, int timeInSeconds) {
        player.setMetadata("temp_lvl", new FixedMetadataValue(this.plugin, player.getLevel()));
        player.setMetadata("temp_exp", new FixedMetadataValue(this.plugin, player.getExp()));
        player.setMetadata("temp_health", new FixedMetadataValue(this.plugin, player.getHealth()));

        new BukkitRunnable() {
            int secondsLeft = timeInSeconds;

            @Override
            public void run() {
                if (!player.isOnline() || AuthGuard.isPlayerLoggedIn(player)) {
                    player.setLevel(player.getMetadata("temp_lvl").get(0).asInt());
                    player.setExp(player.getMetadata("temp_exp").get(0).asFloat());
                    player.setHealth(player.getMetadata("temp_health").get(0).asDouble());

                    player.removeMetadata("temp_lvl", plugin);
                    player.removeMetadata("temp_exp", plugin);
                    player.removeMetadata("temp_health", plugin);

                    cancel();
                    return;
                }

                // Update XP bar to show countdown progress:
                // XP level can be the seconds left,
                // XP progress is fraction of second passed in the current second.
                player.setLevel(secondsLeft);

                // Set XP progress between 0.0 and 1.0 for smooth bar
                // We'll keep it simple: progress goes from 1 (full) to 0 (empty)
                player.setExp(secondsLeft / (float) timeInSeconds);

                boolean loggedIn = AuthGuard.isPlayerLoggedIn(player);
                if (loggedIn) {
                    // Player logged in, reset everything
                    player.setLevel(player.getMetadata("temp_lvl").get(0).asInt());
                    player.setExp(player.getMetadata("temp_exp").get(0).asFloat());
                    player.setHealth(player.getMetadata("temp_health").get(0).asDouble());

                    player.removeMetadata("temp_lvl", plugin);
                    player.removeMetadata("temp_exp", plugin);
                    player.removeMetadata("temp_health", plugin);

                    cancel();
                    return;

                } else if (secondsLeft <= 0) {
                    player.kickPlayer(MessageManager.toLegacy(langManager.getString("kick_timeout")));

                    cancel();
                    return;
                }

                // Play tick sound
                if (secondsLeft % 5 == 0 && secondsLeft != timeInSeconds) {
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                    // displayWelcomeMessage(player);
                }

                secondsLeft--;
            }
        }.runTaskTimer(this.plugin, 0L, 20L); // run every second (20 ticks)
    }
}
