package io.github.openminigameserver.cloudminestom;

import cloud.commandframework.CommandManager;
import cloud.commandframework.CommandTree;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.internal.CommandRegistrationHandler;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import io.github.openminigameserver.cloudminestom.caption.MinestomCaptionRegistry;
import io.github.openminigameserver.cloudminestom.parsers.PlayerArgument;
import io.leangen.geantyref.TypeToken;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Command manager for the Minestom platform
 *
 * @param <C> Command sender type
 */
public class MinestomCommandManager<C> extends CommandManager<C> {
    @NotNull
    private final Function<CommandSender, C> commandSenderMapper;
    @NotNull
    private final Function<C, CommandSender> backwardsCommandSenderMapper;

    /**
     * Create a new command manager instance
     *
     * @param commandExecutionCoordinator Execution coordinator instance. The coordinator is in charge of executing
     *                                    incoming
     *                                    commands. Some considerations must be made when picking a suitable
     *                                    execution coordinator
     *                                    for your platform. For example, an entirely asynchronous coordinator is not
     *                                    suitable
     *                                    when the parsers used in that particular platform are not thread safe. If
     *                                    you have
     *                                    commands that perform blocking operations, however, it might not be a good
     *                                    idea to
     *                                    use a synchronous execution coordinator. In most cases you will want to
     *                                    pick between
     *                                    {@link CommandExecutionCoordinator#simpleCoordinator()} and
     *                                    {@link AsynchronousCommandExecutionCoordinator}
     */
    protected MinestomCommandManager(Function<CommandTree<C>, CommandExecutionCoordinator<C>> commandExecutionCoordinator,
                                     final @NotNull Function<CommandSender, C> commandSenderMapper,
                                     final @NotNull Function<C, CommandSender> backwardsCommandSenderMapper) {
        super(commandExecutionCoordinator, new MinestomCommandRegistrationHandler<>());
        CommandRegistrationHandler registrationHandler = getCommandRegistrationHandler();
        if (registrationHandler instanceof MinestomCommandRegistrationHandler)
            ((MinestomCommandRegistrationHandler<C>) registrationHandler).initialize(this);
        this.commandSenderMapper = commandSenderMapper;
        this.backwardsCommandSenderMapper = backwardsCommandSenderMapper;


        this.getParserRegistry().registerParserSupplier(TypeToken.get(Player.class), parserParameters ->
                new PlayerArgument.PlayerParser<>());

        this.setCaptionRegistry(new MinestomCaptionRegistry<>());
    }

    @NotNull
    public C mapCommandSender(CommandSender sender) {
        return commandSenderMapper.apply(sender);
    }

    @NotNull
    public CommandSender backwardsMapCommandSender(C sender) {
        return backwardsCommandSenderMapper.apply(sender);
    }

    @Override
    public boolean hasPermission(@NotNull C sender,
                                 @NotNull String permission) {
        CommandSender minestomSender = backwardsMapCommandSender(sender);
        return minestomSender.isConsole() || minestomSender.hasPermission(permission);
    }

    @Override
    public @NotNull
    CommandMeta createDefaultCommandMeta() {
        return SimpleCommandMeta.simple().build();
    }
}
