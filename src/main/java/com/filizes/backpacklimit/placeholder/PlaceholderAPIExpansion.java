package com.filizes.backpacklimit.placeholder;

import com.filizes.backpacklimit.Main;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIExpansion extends PlaceholderExpansion implements Listener {

    private final Main plugin;

    public PlaceholderAPIExpansion(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "backpack_limit";
    }

    @Override
    public @NotNull String getAuthor() {
        return "FILIZES";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier.equals("limit")) {
            return String.valueOf(plugin.DatabaseManager().getPlayerBackpackLimit(player.getName()));
        }

        return null;
    }
}