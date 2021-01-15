package io.github.openminigameserver.cloudminestom;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.StaticArgument;
import cloud.commandframework.internal.CommandRegistrationHandler;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class MinestomCommandRegistrationHandler<C> implements CommandRegistrationHandler {

    private MinestomCommandManager<C> commandManager;

    void initialize(final @NotNull MinestomCommandManager<C> cloudburstCommandManager) {
        this.commandManager = cloudburstCommandManager;
    }

    @Override
    public boolean registerCommand(@NotNull Command<?> command) {
        /* We only care about the root command argument */
        final CommandArgument<?, ?> commandArgument = command.getArguments().get(0);

        var label = commandArgument.getName();
        var aliases = new ArrayList<>(((StaticArgument<?>) commandArgument).getAlternativeAliases());

        var commandInstance = MinecraftServer.getCommandManager().getCommand(label);

        if (commandInstance instanceof MinestomCloudCommand) {
            //Register subcommands
            ((MinestomCloudCommand<?>) commandInstance).registerCommandArguments(command);
            return true;
        }

        MinecraftServer.getCommandManager().register(
                new MinestomCloudCommand<>((Command<C>) command,
                        commandManager,
                        label,
                        aliases.toArray(String[]::new)
                )
        );

        return true;
    }
}
