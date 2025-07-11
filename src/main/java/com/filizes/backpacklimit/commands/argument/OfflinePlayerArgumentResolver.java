package com.filizes.backpacklimit.commands.argument;

import com.filizes.backpacklimit.config.service.MessageService;
import com.filizes.backpacklimit.database.interfaces.DatabaseManager;
import com.google.inject.Inject;
import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.*;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class OfflinePlayerArgumentResolver extends ArgumentResolver<CommandSender, OfflinePlayer> {

    private final DatabaseManager databaseManager;
    private final MessageService messageService;

    @Override
    public ParseResult<OfflinePlayer> parse(Invocation<CommandSender> invocation, Argument<OfflinePlayer> context, String argument) {
        Player onlinePlayer = Bukkit.getPlayer(argument);
        if (onlinePlayer != null) {
            return ParseResult.success(onlinePlayer);
        }

        CompletableFuture<OfflinePlayer> future = databaseManager.getPlayerUUID(argument)
                .thenApply(opt -> opt.map(Bukkit::getOfflinePlayer).orElse(null));

        return ParseResult.completableFuture(future, offlinePlayer -> {
            if (offlinePlayer != null && (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline())) {
                return ParseResult.success(offlinePlayer);
            }
            return ParseResult.failure(messageService.getMessage("player_not_found", Map.of("{player}", argument)));
        });
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<OfflinePlayer> argument, SuggestionContext context) {
        String current = context.getCurrent().multilevel();
        String currentLower = current.toLowerCase();
        var suggestions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(currentLower))
                .forEach(suggestions::add);

        try {
            List<String> dbNames = databaseManager.findPlayersByNamePrefix(current)
                    .get(150, TimeUnit.MILLISECONDS);
            suggestions.addAll(dbNames);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        return SuggestionResult.of(suggestions);
    }
}