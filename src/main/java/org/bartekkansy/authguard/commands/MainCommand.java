package org.bartekkansy.authguard.commands;

import org.bartekkansy.authguard.AuthGuard;
import org.bartekkansy.authguard.database.DatabaseManager;
import org.bartekkansy.authguard.event.EventDispatcher;
import org.bartekkansy.authguard.managers.LangManager;
import org.bartekkansy.authguard.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainCommand implements CommandExecutor, TabCompleter {

    private AuthGuard plugin;

    private LangManager langManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;

    public MainCommand(AuthGuard plugin) {
        this.plugin = plugin;

        this.langManager = this.plugin.getLangManager();
        this.messageManager = this.plugin.getMessageManager();
        this.databaseManager = this.plugin.getDatabaseManager();
    }

    private void sendCommandHelp(CommandSender sender, String command, String key) {
        String s1 = this.langManager.getString(key);
        String s2 = this.langManager.getString("help_base", Map.of("cmd", command, "help", s1));
        sender.sendMessage(MessageManager.toLegacy(s2));
    }

    private void displayHelp(CommandSender sender) {
        this.messageManager.sendMessageToSender(sender, "authguard_help", Map.of());

        sendCommandHelp(sender, "/register", "help_register");
        sendCommandHelp(sender, "/login", "help_login");

        sender.sendMessage();

        sendCommandHelp(sender, "/authguard database", "help_authguard_database");
        sendCommandHelp(sender, "/authguard reload", "help_authguard_reload");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("authguard.admin")) {
            this.messageManager.sendMessageToSender(sender, "no_permission_message", Map.of());
            return true;
        }

        if (args.length == 0) {
            this.messageManager.sendMessageToSender(sender, "authguard_usage", Map.of("args", "<database | reload | help>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                displayHelp(sender);
                break;

            case "reload":
                this.plugin.reloadConfig();
                this.langManager.reload();
                this.databaseManager.disconnect();
                EventDispatcher.init();
                try {
                    this.databaseManager.connect();
                } catch (SQLException e) {
                    this.messageManager.sendMessageToSender(sender, "authguard_reload_error", Map.of("error_code", "$SQL.database.connect()"));
                    throw new RuntimeException(e);
                }

                this.messageManager.sendMessageToSender(sender, "authguard_reload", Map.of());
                break;

            case "database":
                if (args.length < 2) {
                    this.messageManager.sendMessageToSender(sender, "authguard_usage", Map.of("args", "database <remove>"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("remove")) {
                    if (args.length < 3) {
                        this.messageManager.sendMessageToSender(sender, "authguard_usage", Map.of("args", "database [player]"));
                        return true;
                    }
                    String targetPlayer = args[2];
                    UUID uuid = null;

                    // Try to parse as UUID
                    try {
                        uuid = UUID.fromString(targetPlayer);
                    } catch (IllegalArgumentException e) {
                        // Not a UUID, try to get UUID by player name
                        Player onlinePlayer = Bukkit.getPlayerExact(targetPlayer);
                        if (onlinePlayer != null) {
                            uuid = onlinePlayer.getUniqueId();
                        }
                    }

                    if (uuid == null) {
                        this.messageManager.sendMessageToSender(sender, "authguard_database_unknown_player", Map.of("player", targetPlayer));
                        return true;
                    }

                    // Remove player from database here
                    try {
                        boolean result = this.databaseManager.removePlayer(uuid.toString());
                        if (result) {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null) player.kickPlayer(MessageManager.toLegacy(this.langManager.getString("kick_database_remove")));
                            this.messageManager.sendMessageToSender(sender, "authguard_database_remove_success", Map.of("player", targetPlayer));
                            return true;
                        } else {
                            this.messageManager.sendMessageToSender(sender, "authguard_database_remove_error", Map.of("player", targetPlayer));
                            return true;
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    this.messageManager.sendMessageToSender(sender, "authguard_unknown_command", Map.of("args", "database"));
                }
                break;

            default:
                this.messageManager.sendMessageToSender(sender, "authguard_unknown_subcommand", Map.of());
                break;
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("database", "reload", "help");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("database")) {
                return List.of("remove");
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("database")) {
                if (args[1].equalsIgnoreCase("remove")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                }
            }
        }
        return List.of();
    }
}
