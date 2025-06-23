package org.bartekkansy.authguard;

import org.bartekkansy.authguard.commands.ChangePassCommand;
import org.bartekkansy.authguard.commands.LoginCommand;
import org.bartekkansy.authguard.commands.MainCommand;
import org.bartekkansy.authguard.commands.RegisterCommand;
import org.bartekkansy.authguard.database.DatabaseManager;
import org.bartekkansy.authguard.event.EventListener;
import org.bartekkansy.authguard.event.EventDispatcher;
import org.bartekkansy.authguard.managers.LangManager;
import org.bartekkansy.authguard.managers.MessageManager;
import org.bartekkansy.authguard.managers.MetricsManager;
import org.bartekkansy.authguard.utils.LoginState;
import org.bartekkansy.authguard.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.UUID;

public final class AuthGuard extends JavaPlugin {

    private DatabaseManager databaseManager;

    private LangManager langManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        long startTime = System.currentTimeMillis();

        // Initialize Language Manager
        this.langManager = new LangManager(this);

        // Initialize Message Manager
        this.messageManager = new MessageManager(this);

        // Log when plugin is enabled
        this.messageManager.sendMessageToConsole("");
        this.messageManager.sendMessageToConsole("   <gold> █████<gray>╗  <red>██████<gray>╗");
        this.messageManager.sendMessageToConsole("   <gold>██<gray>╔══</gray>██<gray>╗</gray><red>██<gray>╔════╝");
        this.messageManager.sendMessageToConsole("   <gold>███████<gray>║<red>██<gray>║</gray>  ███<gray>╗    <gold>AuthGuard</gold> <red>v" + this.getDescription().getVersion());
        this.messageManager.sendMessageToConsole("   <gold>██<gray>╔══</gray>██<gray>║</gray><red>██<gray>║<gray>   <red>██<gray>║    <yellow>Created by</yellow> <blue>" + String.join(", ", this.getDescription().getAuthors()));
        this.messageManager.sendMessageToConsole("   <gold>██<gray>║</gray>  ██<gray>║╚</gray><red>██████<gray>╔╝");
        this.messageManager.sendMessageToConsole("   <gray>╚═╝  ╚═╝ ╚═════╝");
        this.messageManager.sendMessageToConsole("");

        this.messageManager.sendMessageToConsole("<white> * Initializing Skript...");
        initEventDispatcher();

        this.messageManager.sendMessageToConsole("<white> * Initializing database...");

        // Initialize Database Manager
        databaseManager = new DatabaseManager(this);
        try {
            databaseManager.connect();
        } catch (SQLException e) {
            getLogger().severe("Could not connect to the database! --- Disabling plugin...");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        ///

        // Register all events
        this.messageManager.sendMessageToConsole("<white> * Initializing events...");
        getServer().getPluginManager().registerEvents(new EventListener(this), this);

        this.messageManager.sendMessageToConsole("<white> * Initializing commands...");

        // Register all commands
        this.getCommand("register").setExecutor(new RegisterCommand(this));
        this.getCommand("login").setExecutor(new LoginCommand(this));
        this.getCommand("changepass").setExecutor(new ChangePassCommand(this));
        this.getCommand("authguard").setExecutor(new MainCommand(this));

        long eta = System.currentTimeMillis() - startTime;

        this.messageManager.sendMessageToConsole(String.format("<white> * Loading complete! (%d ms)", eta));

        // Initialize bStats - for data collection
        MetricsManager.start(this);
    }

    public static void initEventDispatcher() {
        if (!Util.isPluginEnabled("Skript")) {
            Bukkit.getLogger().warning("Skript plugin not found or not enabled — Skript events will not be dispatched.");
            return;
        }

        EventDispatcher.init();
    }

    public static void fireEvent(Event event) {
        if (!Util.isPluginEnabled("Skript")) return;

        EventDispatcher.fireEvent(event);
    }

    public static boolean isPlayerLoggedIn(Player player) {
        return player.getMetadata("logged_in").get(0).asBoolean();
    }

    public static LoginState getPlayerLoginState(Player player) {
        return LoginState.valueOf(player.getMetadata("login_state").get(0).asString());
    }

    public boolean isPlayerPremium(Player player) {
        // Offline mode UUIDs are generated with this pattern: UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes())
        // So, if the UUID matches the offline mode pattern, player is not premium
        UUID offlineUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + player.getName()).getBytes());

        return !player.getUniqueId().equals(offlineUUID);
    }

    @Override
    public void onDisable() {
        // Disconnect database
        this.databaseManager.disconnect();

        // Shutdown bStats
        MetricsManager.shutdown();

        // Plugin shutdown logic
        getLogger().info("AuthGuard disabled!");
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public LangManager getLangManager() {
        return this.langManager;
    }

    public MessageManager getMessageManager() {
        return this.messageManager;
    }
}
