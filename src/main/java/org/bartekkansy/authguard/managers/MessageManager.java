package org.bartekkansy.authguard.managers;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bartekkansy.authguard.AuthGuard;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class MessageManager {

    private AuthGuard plugin;

    private LangManager langManager;

    private BukkitAudiences audiences;
    private MiniMessage miniMessage;

    private String messagePrefix;

    public MessageManager(AuthGuard plugin) {
        this.plugin = plugin;
        this.messagePrefix = this.plugin.getConfig().getString("main.message_prefix");

        this.langManager = this.plugin.getLangManager();

        this.audiences = BukkitAudiences.create(this.plugin);
        this.miniMessage = MiniMessage.miniMessage();
    }

    public static String toLegacy(String miniMessageString) {
        Component component = MiniMessage.miniMessage().deserialize(miniMessageString);

        // Now convert to legacy Minecraft format (with § codes)
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public void sendMessageToPlayer(Player player, String message) {
        Component component = miniMessage.deserialize(messagePrefix + "<reset>" + message);
        audiences.player(player).sendMessage(component);
    }

    public void sendMessageToPlayer(Player player, String lang_key, Map<String, String> placeholders) {
        String message = this.langManager.getString(lang_key, placeholders);
        sendMessageToPlayer(player, message);
    }

    public void sendMessageToSender(CommandSender sender, String message) {
        Component component = miniMessage.deserialize(messagePrefix + "<reset>" + message);
        audiences.sender(sender).sendMessage(component);
    }

    public void sendMessageToSender(CommandSender sender, String lang_key, Map<String, String> placeholders) {
        String message = this.langManager.getString(lang_key, placeholders);
        sendMessageToSender(sender, message);
    }

    public void sendMessageToConsole(String message) {
        Component component = miniMessage.deserialize(message);
        audiences.console().sendMessage(component);
    }

    public void sendMessageToConsole(String lang_key, Map<String, String> placeholders) {
        String message = this.langManager.getString(lang_key, placeholders);
        sendMessageToConsole(message);
    }

    public void sendMessageToAll(String message) {
        Component component = miniMessage.deserialize(messagePrefix + "<reset>" + message);
        audiences.all().sendMessage(component);
    }

    public void sendMessageToAll(String lang_key, Map<String, String> placeholders) {
        String message = this.langManager.getString(lang_key, placeholders);
        sendMessageToAll(message);
    }
}
