package com.filizes.backpacklimit.database.interfaces;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DatabaseManager {
    void connect();
    void disconnect();

    CompletableFuture<Optional<UUID>> getPlayerUUID(String playerName);
    CompletableFuture<Integer> getPlayerBackpackLimit(UUID playerUUID);
    CompletableFuture<Void> setPlayerBackpackLimit(OfflinePlayer player, int newLimit);
    CompletableFuture<Void> addPlayerBackpackLimit(OfflinePlayer player, int amount);
    CompletableFuture<Void> removePlayerBackpackLimit(OfflinePlayer player, int amount);
    CompletableFuture<Void> ensurePlayerExists(Player player);

    CompletableFuture<List<String>> findPlayersByNamePrefix(String prefix);
}