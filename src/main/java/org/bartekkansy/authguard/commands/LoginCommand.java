package org.bartekkansy.simplelogin.commands;

import org.bartekkansy.simplelogin.managers.LangManager;
import org.bartekkansy.simplelogin.LoginState;
import org.bartekkansy.simplelogin.managers.MessageManager;
import org.bartekkansy.simplelogin.AuthGuard;
import org.bartekkansy.simplelogin.database.DatabaseManager;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class LoginCommand implements CommandExecutor, TabCompleter {

    private AuthGuard plugin;

    private LangManager langManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;

    public LoginCommand(AuthGuard plugin) {
        this.plugin = plugin;

        this.langManager = this.plugin.getLangManager();
        this.messageManager = this.plugin.getMessageManager();
        this.databaseManager = this.plugin.getDatabaseManager();
    }

    private boolean onLoginFailed(Player player) {
        int tries = player.getMetadata("login_tries").get(0).asInt() + 1;
        if (tries >= this.plugin.getConfig().getInt("main.login.tries")) {
            player.kickPlayer(MessageManager.toLegacy(this.langManager.getString("kick_login_tries")));
            return true;
        }

        player.setMetadata("login_tries", new FixedMetadataValue(this.plugin, tries));
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
        this.messageManager.sendMessageToPlayer(player, "login_password_error", Map.of());
        return true;
    }

    private boolean onLoginSuccess(Player player) {
        player.playSound(player, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 1.0f);
        player.setMetadata("logged_in", new FixedMetadataValue(this.plugin, true));
        this.messageManager.sendMessageToPlayer(player, "login_success_message", Map.of());

        player.removeMetadata("captcha", this.plugin);
        player.removeMetadata("login_tries", this.plugin);
        return true;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (AuthGuard.isPlayerLoggedIn(player)) {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            this.messageManager.sendMessageToPlayer(player, "login_no_need_message", Map.of());
            return true;
        } else if (AuthGuard.getPlayerLoginState(player) == LoginState.REGISTER) {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            this.messageManager.sendMessageToPlayer(player, "login_before_register_message", Map.of());
            return true;
        }

        if (args.length != 1) {
            this.messageManager.sendMessageToPlayer(player, "login_usage", Map.of("args", String.format("<%s>", this.langManager.getString("password"))));
            return true;
        }

        String password = args[0];
        try {
            if (this.databaseManager.isPasswordCorrect(player.getUniqueId().toString(), password)) {
                return onLoginSuccess(player);

            } else {
                return onLoginFailed(player);
            }
        } catch (SQLException e) {
            player.kickPlayer(MessageManager.toLegacy(langManager.getString("kick_error", Map.of("error_code", "$SQL.login.onCommand()"))));
            throw new RuntimeException(e);
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of();
    }
}
