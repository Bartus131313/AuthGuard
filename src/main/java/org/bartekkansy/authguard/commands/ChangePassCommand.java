package org.bartekkansy.authguard.commands;

import org.bartekkansy.authguard.AuthGuard;
import org.bartekkansy.authguard.database.DatabaseManager;
import org.bartekkansy.authguard.managers.LangManager;
import org.bartekkansy.authguard.managers.MessageManager;
import org.bukkit.Sound;
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

public class ChangePassCommand implements CommandExecutor, TabCompleter {

    private AuthGuard plugin;

    private LangManager langManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;

    public ChangePassCommand(AuthGuard plugin) {
        this.plugin = plugin;

        this.langManager = this.plugin.getLangManager();
        this.messageManager = this.plugin.getMessageManager();
        this.databaseManager = this.plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;

        if (!AuthGuard.isPlayerLoggedIn(player)) {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            this.messageManager.sendMessageToPlayer(player, "changepass_login_error", Map.of());
            return true;
        }

        if (args.length != 3) {
            this.messageManager.sendMessageToPlayer(player, "changepass_usage", Map.of());
            return true;
        }

        String current_password = args[0];
        String new_password1 = args[1];
        String new_password2 = args[2];

        try {
            if (!this.databaseManager.isPasswordCorrect(player.getUniqueId().toString(), current_password)) {
                this.messageManager.sendMessageToPlayer(player, "changepass_current_error", Map.of());
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (!new_password1.equals(new_password2)) {
            this.messageManager.sendMessageToPlayer(player, "changepass_new_error", Map.of());
            return true;
        }

        if (new_password1.length() < 6 || new_password1.length() > 32) {
            this.messageManager.sendMessageToPlayer(player, "register_password_length_error", Map.of());
            return true;
        }

        try {
            this.databaseManager.savePlayerPassword(player.getUniqueId().toString(), new_password1);
            player.kickPlayer(MessageManager.toLegacy(this.langManager.getString("kick_changepass_success")));

        } catch (SQLException e) {
            player.kickPlayer(MessageManager.toLegacy(langManager.getString("kick_error", Map.of("error_code", "$SQL.changepass.onCommand()"))));
            throw new RuntimeException(e);
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of();
    }
}
