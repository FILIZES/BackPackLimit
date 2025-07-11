package com.filizes.backpacklimit.commands.handler;

import com.filizes.backpacklimit.config.service.MessageService;
import com.google.inject.Inject;
import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invalidusage.InvalidUsage;
import dev.rollczi.litecommands.invalidusage.InvalidUsageHandler;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.schematic.Schematic;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;

import java.util.Map;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class CustomInvalidUsageHandler implements InvalidUsageHandler<CommandSender> {

    private final MessageService messageService;

    @Override
    public void handle(Invocation<CommandSender> invocation, InvalidUsage<CommandSender> invalidUsage, ResultHandlerChain<CommandSender> chain) {
        CommandSender sender = invocation.sender();
        Schematic schematic = invalidUsage.getSchematic();

        if (schematic.all().isEmpty()) {
            messageService.sendMessage(sender, "no_subcommands_available");
            return;
        }

        if (schematic.isOnlyFirst()) {
            messageService.sendMessage(sender, "invalid_usage.single", Map.of("{usage}", schematic.first()));
            return;
        }

        messageService.sendMessage(sender, "invalid_usage.header");
        schematic.all().stream()
                .map(scheme -> Map.of("{usage}", scheme))
                .forEach(placeholders -> messageService.sendMessage(sender, "invalid_usage.line", placeholders));
    }
}