package org.bartekkansy.authguard.event;

import org.bartekkansy.authguard.managers.LangManager;
import org.bartekkansy.authguard.utils.LoginState;
import org.bartekkansy.authguard.managers.MessageManager;
import org.bartekkansy.authguard.AuthGuard;
import org.bartekkansy.authguard.commands.RegisterCommand;
import org.bartekkansy.authguard.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class EventListener implements Listener {

    private AuthGuard plugin;

    private DatabaseManager databaseManager;
    private LangManager langManager;
    private MessageManager messageManager;

    private Map<Player, BossBar> timeLeftBossBars = new HashMap<>();

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
        if (plugin.getConfig().getBoolean("main.sounds.player_join"))
            player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 0.8f, 1.0f);

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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        BossBar bar = timeLeftBossBars.get(player);
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
            timeLeftBossBars.remove(player);
        }
    }
    ///

    private void kickPlayerAfterTime(Player player, int timeInSeconds) {
        String title = this.langManager.getString("bossbar_timeout", Map.of("time", String.valueOf(timeInSeconds)));
        BossBar bar = Bukkit.createBossBar(MessageManager.toLegacy(title), BarColor.PINK, BarStyle.SEGMENTED_20, BarFlag.DARKEN_SKY, BarFlag.CREATE_FOG);
        bar.addPlayer(player);
        timeLeftBossBars.put(player, bar);

        new BukkitRunnable() {
            int secondsLeft = timeInSeconds;

            @Override
            public void run() {
                if (!player.isOnline() || AuthGuard.isPlayerLoggedIn(player)) {
                    bar.removePlayer(player);
                    bar.setVisible(false);
                    timeLeftBossBars.remove(player);

                    cancel();
                    return;
                }

                bar.setProgress((double) secondsLeft / timeInSeconds);

                String title = langManager.getString("bossbar_timeout", Map.of("time", String.valueOf(secondsLeft)));
                bar.setTitle(MessageManager.toLegacy(title));

                boolean loggedIn = AuthGuard.isPlayerLoggedIn(player);
                if (loggedIn) {
                    bar.removeAll();
                    bar.setVisible(false);
                    timeLeftBossBars.remove(player);

                    cancel();
                    return;

                } else if (secondsLeft <= 0) {
                    bar.removeAll();
                    bar.setVisible(false);
                    timeLeftBossBars.remove(player);

                    player.kickPlayer(MessageManager.toLegacy(langManager.getString("kick_timeout")));

                    cancel();
                    return;
                }

                // Play tick sound
                if (secondsLeft % 5 == 0 && secondsLeft != timeInSeconds) {
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }

                secondsLeft--;
            }
        }.runTaskTimer(this.plugin, 0L, 20L); // run every second (20 ticks)
    }
}
