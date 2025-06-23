package org.bartekkansy.authguard.commands;

import org.bartekkansy.authguard.*;
import org.bartekkansy.authguard.database.DatabaseManager;
import org.bartekkansy.authguard.event.EventDispatcher;
import org.bartekkansy.authguard.event.custom.PlayerRegisteredEvent;
import org.bartekkansy.authguard.managers.LangManager;
import org.bartekkansy.authguard.managers.MessageManager;
import org.bartekkansy.authguard.utils.CaptchaGenerator;
import org.bartekkansy.authguard.utils.LoginState;
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

public class RegisterCommand implements CommandExecutor, TabCompleter {

    private AuthGuard plugin;

    private LangManager langManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;

    public RegisterCommand(AuthGuard plugin) {
        this.plugin = plugin;

        this.langManager = this.plugin.getLangManager();
        this.messageManager = this.plugin.getMessageManager();
        this.databaseManager = this.plugin.getDatabaseManager();
    }

    public static void setNewCaptchaForPlayer(AuthGuard plugin, Player player) {
        String captcha = CaptchaGenerator.generateCaptcha(6);
        player.setMetadata("captcha", new FixedMetadataValue(plugin, captcha));

        player.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        String s = MessageManager.toLegacy(plugin.getLangManager().getString("captcha_message"));
        String s1 = MessageManager.toLegacy("<blue>" + captcha);

        player.sendTitle(s, s1, 10, (plugin.getConfig().getInt("main.kick_timeout") + 5) * 20, 10);
    }

    public static String getPlayerCaptcha(Player player) {
        if (player.hasMetadata("captcha")) return player.getMetadata("captcha").get(0).asString();
        return null;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;

        if (AuthGuard.isPlayerLoggedIn(player)) {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            this.messageManager.sendMessageToPlayer(player, "register_no_need", Map.of());
            return true;
        } else if (AuthGuard.getPlayerLoginState(player) == LoginState.LOGIN) {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            this.messageManager.sendMessageToPlayer(player, "register_after_login_message", Map.of());
            return true;
        }

        boolean v_repeatPassword = this.plugin.getConfig().getBoolean("main.register.repeat_password");
        boolean v_captcha = this.plugin.getConfig().getBoolean("main.register.captcha");

        int argsLength = 1 + (v_repeatPassword ? 1 : 0) + (v_captcha ? 1 : 0);
        if (args.length != argsLength) {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("<%s>", this.langManager.getString("password")));
            if (v_repeatPassword) builder.append(String.format(" <%s>", this.langManager.getString("password")));
            if (v_captcha) builder.append(String.format(" <%s>", this.langManager.getString("captcha")));

            this.messageManager.sendMessageToPlayer(player, "register_usage", Map.of("args", builder.toString()));
            return true;
        }

        String password = args[0];
        if (v_repeatPassword) {
            if (!password.equals(args[1])) {
                this.messageManager.sendMessageToPlayer(player, "register_passwords_error", Map.of());
                return true;
            }
        }

        if (password.length() < 6 || password.length() > 32) {
            this.messageManager.sendMessageToPlayer(player, "register_password_length_error", Map.of());
            return true;
        }

        String playerCaptcha = getPlayerCaptcha(player);
        if (v_captcha) {
            if (!args[2].equals(playerCaptcha)) {
                this.messageManager.sendMessageToPlayer(player, "register_captcha_error", Map.of());
                setNewCaptchaForPlayer(this.plugin, player);
                return true;
            }
        }

        try {
            this.databaseManager.savePlayerPassword(player.getUniqueId().toString(), password);

            // Send event through Skript
            EventDispatcher.fireEvent(new PlayerRegisteredEvent(player));

            player.kickPlayer(MessageManager.toLegacy(this.langManager.getString("kick_register_success")));

        } catch (SQLException e) {
            player.kickPlayer(MessageManager.toLegacy(langManager.getString("kick_error", Map.of("error_code", "$SQL.register.onCommand()"))));
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
