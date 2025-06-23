package org.bartekkansy.simplelogin;

import org.bartekkansy.simplelogin.commands.LoginCommand;
import org.bartekkansy.simplelogin.commands.MainCommand;
import org.bartekkansy.simplelogin.commands.RegisterCommand;
import org.bartekkansy.simplelogin.database.DatabaseManager;
import org.bartekkansy.simplelogin.event.EventListener;
import org.bartekkansy.simplelogin.managers.LangManager;
import org.bartekkansy.simplelogin.managers.MessageManager;
import org.bukkit.entity.Player;
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

        // Initialize Language Manager
        this.langManager = new LangManager(this);

        // Initialize Message Manager
        this.messageManager = new MessageManager(this);

        // Register all events
        getServer().getPluginManager().registerEvents(new EventListener(this), this);

        // Register all commands
        this.getCommand("register").setExecutor(new RegisterCommand(this));
        this.getCommand("login").setExecutor(new LoginCommand(this));
        this.getCommand("simplelogin").setExecutor(new MainCommand(this));

        // Log when plugin is enabled
        getLogger().info("SimpleLogin enabled!");
    }

    public static boolean isPlayerLoggedIn(Player player) {
        return player.getMetadata("logged_in").getFirst().asBoolean();
    }

    public static LoginState getPlayerLoginState(Player player) {
        return LoginState.valueOf(player.getMetadata("login_state").getFirst().asString());
    }

    public boolean isPlayerPremium(Player player) {
        // Offline mode UUIDs are generated with this pattern: UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes())
        // So, if the UUID matches the offline mode pattern, player is not premium
        UUID offlineUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + player.getName()).getBytes());

        return !player.getUniqueId().equals(offlineUUID);
    }

    @Override
    public void onDisable() {
        this.databaseManager.disconnect();

        // Plugin shutdown logic
        getLogger().info("SimpleLogin disabled!");
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
