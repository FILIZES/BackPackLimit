package com.filizes.backpacklimit.commands;

import com.filizes.backpacklimit.Main;
import com.filizes.backpacklimit.config.service.MessageService;
import com.filizes.backpacklimit.config.settings.LimitSettings;
import com.filizes.backpacklimit.database.interfaces.DatabaseManager;
import com.filizes.backpacklimit.listener.service.InventoryLimitService;
import com.google.inject.Inject;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Command(name = "backpacklimit", aliases = "bpl")
public final class BackpackLimitCommand {

    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final MessageService messageService;
    private final InventoryLimitService limitService;

    private String getPlayerName(OfflinePlayer player) {
        return Optional.ofNullable(player.getName()).orElse(player.getUniqueId().toString());
    }

    @Execute(name = "info")
    @Permission("backpacklimit.info")
    public void info(@Context CommandSender sender, @Arg("player") OfflinePlayer player) {
        String playerName = getPlayerName(player);
        databaseManager.getPlayerBackpackLimit(player.getUniqueId())
                .thenAccept(limit -> messageService.sendMessage(sender, "current_limit", Map.of(
                        "{player}", playerName,
                        "{limit}", String.valueOf(limit)
                )))
                .exceptionally(handleFailure(sender, playerName));
    }

    @Execute(name = "set")
    @Permission("backpacklimit.set")
    public void set(@Context CommandSender sender, @Arg("player") OfflinePlayer player, @Arg("amount") int amount) {
        if (amount < 0) {
            messageService.sendMessage(sender, "amount_must_be_positive_or_zero");
            return;
        }
        executeUpdate(sender, player,
                () -> databaseManager.setPlayerBackpackLimit(player, amount),
                "set_limit", Map.of("{limit}", String.valueOf(amount))
        );
    }

    @Execute(name = "add")
    @Permission("backpacklimit.add")
    public void add(@Context CommandSender sender, @Arg("player") OfflinePlayer player, @Arg("amount") int amount) {
        if (amount <= 0) {
            messageService.sendMessage(sender, "amount_must_be_positive");
            return;
        }
        executeUpdate(sender, player,
                () -> databaseManager.addPlayerBackpackLimit(player, amount),
                "increase_limit", Map.of("{amount}", String.valueOf(amount))
        );
    }

    @Execute(name = "remove")
    @Permission("backpacklimit.remove")
    public void remove(@Context CommandSender sender, @Arg("player") OfflinePlayer player, @Arg("amount") int amount) {
        if (amount <= 0) {
            messageService.sendMessage(sender, "amount_must_be_positive");
            return;
        }
        executeUpdate(sender, player,
                () -> databaseManager.removePlayerBackpackLimit(player, amount),
                "decrease_limit", Map.of("{amount}", String.valueOf(amount))
        );
    }

    private void executeUpdate(CommandSender sender, OfflinePlayer player, Supplier<CompletableFuture<Void>> dbOperation, String messageKey, Map<String, String> placeholders) {
        String playerName = getPlayerName(player);
        dbOperation.get()
                .thenCompose(v -> databaseManager.getPlayerBackpackLimit(player.getUniqueId()))
                .thenAccept(newLimit -> handleLimitUpdateSuccess(sender, player, messageKey, newLimit, placeholders))
                .exceptionally(handleFailure(sender, playerName));
    }

    private void handleLimitUpdateSuccess(CommandSender sender, OfflinePlayer player, String messageKey, int newLimit, Map<String, String> placeholders) {
        Map<String, String> finalPlaceholders = new java.util.HashMap<>(placeholders);
        finalPlaceholders.put("{player}", getPlayerName(player));

        messageService.sendMessage(sender, messageKey, finalPlaceholders);
        limitService.updatePlayerLimitInCache(player.getUniqueId(), newLimit);

        Optional.ofNullable(player.getPlayer()).ifPresent(limitService::checkAndCorrectInventoryAsync);
    }

    private Function<Throwable, Void> handleFailure(CommandSender sender, String playerName) {
        return ex -> {
            messageService.sendMessage(sender, "error_modifying_limit", Map.of("{player}", playerName));
            return null;
        };
    }

    @Execute(name = "reload")
    @Permission("backpacklimit.reload")
    public void reload(@Context CommandSender sender) {
        plugin.reloadConfig();
        messageService.reloadMessages();
        FileConfiguration newConfig = plugin.getConfig();
        LimitSettings newLimitSettings = new LimitSettings(
                newConfig.getInt("limit-settings.default-backpack-limit", 8),
                newConfig.getBoolean("limit-settings.ignore-armor-and-offhand", true)
        );

        limitService.reload(newLimitSettings);

        messageService.sendMessage(sender, "reload_success");
    }

}