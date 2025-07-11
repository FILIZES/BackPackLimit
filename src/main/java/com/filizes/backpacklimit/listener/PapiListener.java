package com.filizes.backpacklimit.listener;

import com.filizes.backpacklimit.listener.service.InventoryLimitService;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class PapiListener extends PlaceholderExpansion {

    private final InventoryLimitService limitService;

    @Override
    public @NotNull String getIdentifier() { return "backpacklimit"; }

    @Override
    public @NotNull String getAuthor() { return "FILIZES"; }

    @Override
    public @NotNull String getVersion() { return "2.0.0"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        if ("limit".equalsIgnoreCase(params)) {
            return String.valueOf(limitService.getCachedLimit(player.getUniqueId()));
        }

        return null;
    }
}