package com.filizes.backpacklimit.loader;

import com.filizes.backpacklimit.Main;
import com.filizes.backpacklimit.commands.BackpackLimitCommand;
import com.filizes.backpacklimit.commands.argument.OfflinePlayerArgumentResolver;
import com.filizes.backpacklimit.commands.handler.CustomInvalidUsageHandler;
import com.filizes.backpacklimit.config.service.MessageService;
import com.google.inject.Injector;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class CommandsLoader {

    private LiteCommands<CommandSender> liteCommands;

    public CommandsLoader(Main plugin, Injector injector) {
        this.liteCommands = createLiteCommands(plugin, injector);
    }

    public void load() {
    }

    public void unload() {
        if (this.liteCommands != null) {
            this.liteCommands.unregister();
        }
    }

    private LiteCommands<CommandSender> createLiteCommands(Main plugin, Injector injector) {
        MessageService messageService = injector.getInstance(MessageService.class);
        return LiteBukkitFactory.builder(plugin)
                .commands(injector.getInstance(BackpackLimitCommand.class))
                .argument(OfflinePlayer.class, injector.getInstance(OfflinePlayerArgumentResolver.class))
                .invalidUsage(injector.getInstance(CustomInvalidUsageHandler.class))
                .missingPermission((invocation, missingPermissions, chain) ->
                        messageService.sendMessage(invocation.sender(), "no_permission"))
                .build();
    }
}