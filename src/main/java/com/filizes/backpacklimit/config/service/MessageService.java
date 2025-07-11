package com.filizes.backpacklimit.config.service;

import com.filizes.backpacklimit.Main;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class MessageService {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    Main plugin;
    File messagesFile;
    Set<UUID> messageCooldowns = Sets.newConcurrentHashSet();
    long throttleCooldownTicks;

    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static class MutableState {
        FileConfiguration messagesConfig;
        String prefix;
    }

    MutableState state = new MutableState();

    public MessageService(Main plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        reloadMessages();
        this.throttleCooldownTicks = plugin.getConfig().getLong("messages.throttle-cooldown-seconds", 3L) * 20L;
    }

    public void reloadMessages() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.state.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        this.state.prefix = this.state.messagesConfig.getString("prefix", "&c[BPL] &r");
    }

    private String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        var matcher = HEX_PATTERN.matcher(text);
        var buffer = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = state.messagesConfig.getString(key, "&cСообщение не найдено: " + key);
        for (var entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        message = message.replace("{prefix}", this.state.prefix);
        return colorize(message);
    }

    public String getMessage(String key) {
        return getMessage(key, Map.of());
    }

    public void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = getMessage(key, placeholders);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, Map.of());
    }

    public void sendThrottledMessage(Player player, String key, Map<String, String> placeholders) {
        if (messageCooldowns.add(player.getUniqueId())) {
            sendMessage(player, key, placeholders);
            Bukkit.getScheduler().runTaskLater(plugin, () -> messageCooldowns.remove(player.getUniqueId()), throttleCooldownTicks);
        }
    }
}