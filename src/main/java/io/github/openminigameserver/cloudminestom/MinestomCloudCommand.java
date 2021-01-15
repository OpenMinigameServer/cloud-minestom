package io.github.openminigameserver.cloudminestom;

import cloud.commandframework.exceptions.*;
import kotlin.collections.ArraysKt;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Arguments;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.CommandSyntax;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentDynamicStringArray;
import net.minestom.server.command.builder.arguments.ArgumentDynamicWord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static io.github.openminigameserver.cloudminestom.CloudInteropHelper.removeSlashPrefix;

public class MinestomCloudCommand<C> extends Command {

    private static final String MESSAGE_INTERNAL_ERROR = "An internal error occurred while attempting to perform this" +
            " command.";
    private static final String MESSAGE_NO_PERMS =
            "I'm sorry, but you do not have permission to perform this command. "
                    + "Please contact the server administrators if you believe that this is in error.";
    private static final String MESSAGE_UNKNOWN_COMMAND = "Unknown command. Type \"/help\" for help.";

    private final MinestomCommandManager<C> manager;
    private final CommandExecutor emptyExecutor = (sender, args) -> {
    };
    private Boolean isAmbiguous = false;

    public MinestomCloudCommand(cloud.commandframework.Command<C> cloudCommand, MinestomCommandManager<C> manager,
                                String name, String... aliases) {
        super(name, aliases);
        this.manager = manager;

        if (cloudCommand.isHidden()) {
            setCondition((sender, commandString) -> commandString != null);
        }

        registerCommandArguments(cloudCommand);
    }

    @NotNull
    private static String[] getArgumentNamesFromArguments(Argument<?>[] arguments) {
        return Arrays.stream(arguments).map(Argument::getId).toArray(String[]::new);
    }

    public void registerCommandArguments(cloud.commandframework.Command<?> cloudCommand) {
        //Do not register arguments if command is ambiguous
        if (isAmbiguous) return;
        setDefaultExecutor(emptyExecutor);

        var arguments =
                cloudCommand.getArguments().stream()
                        .skip(1)
                        .map(CloudInteropHelper::convertCloudArgumentToMinestom)
                        .toArray(Argument[]::new);

        var containsSyntax =
                getSyntaxes().stream().anyMatch(syntax -> Arrays.equals(getArgumentNamesFromArguments(arguments),
                        getArgumentNamesFromArguments(syntax.getArguments())));

        var syntaxes = (List<CommandSyntax>) getSyntaxes();

        if (!containsSyntax && arguments.length != 0) {
            addSyntax(emptyExecutor, arguments);

            var toMove = syntaxes.stream().filter(it ->
                    Arrays.stream(it.getArguments()).allMatch(arg -> arg instanceof ArgumentDynamicWord)
            ).collect(Collectors.toList());

            syntaxes.removeAll(toMove);
            syntaxes.addAll(0, toMove);

        }
        fixSyntaxArguments(syntaxes);
    }

    private void fixSyntaxArguments(List<CommandSyntax> syntaxes) {
        if (isAmbiguous) return;

        isAmbiguous = syntaxes.stream().anyMatch(it -> ArraysKt.indexOfFirst(it.getArguments(),
                arg -> arg instanceof ArgumentDynamicWord) == 0);

        if (isAmbiguous) {
            syntaxes.clear();
            addSyntax(emptyExecutor, new ArgumentDynamicStringArray("args"));
        }
    }

    @Override
    public void globalListener(@NotNull CommandSender commandSender, @NotNull Arguments arguments,
                               @NotNull String command) {
        var input = CloudInteropHelper.removeSlashPrefix(command);
        final C sender = this.manager.mapCommandSender(commandSender);
        this.manager.executeCommand(
                sender,
                input
        )
                .whenComplete((commandResult, throwable) -> {
                    if (throwable != null) {
                        if (throwable instanceof CompletionException) {
                            throwable = throwable.getCause();
                        }
                        final Throwable finalThrowable = throwable;
                        if (throwable instanceof InvalidSyntaxException) {
                            this.manager.handleException(sender,
                                    InvalidSyntaxException.class,
                                    (InvalidSyntaxException) throwable, (c, e) ->
                                            commandSender.sendMessage(
                                                    "Invalid Command Syntax. "
                                                            + "Correct command syntax is: "
                                                            + "/"
                                                            + ((InvalidSyntaxException) finalThrowable)
                                                            .getCorrectSyntax())
                            );
                        } else if (throwable instanceof InvalidCommandSenderException) {
                            this.manager.handleException(sender,
                                    InvalidCommandSenderException.class,
                                    (InvalidCommandSenderException) throwable, (c, e) ->
                                            commandSender.sendMessage(finalThrowable.getMessage())
                            );
                        } else if (throwable instanceof NoPermissionException) {
                            this.manager.handleException(sender,
                                    NoPermissionException.class,
                                    (NoPermissionException) throwable, (c, e) ->
                                            commandSender.sendMessage(MESSAGE_NO_PERMS)
                            );
                        } else if (throwable instanceof NoSuchCommandException) {
                            this.manager.handleException(sender,
                                    NoSuchCommandException.class,
                                    (NoSuchCommandException) throwable, (c, e) ->
                                            commandSender.sendMessage(MESSAGE_UNKNOWN_COMMAND)
                            );
                        } else if (throwable instanceof ArgumentParseException) {
                            this.manager.handleException(sender,
                                    ArgumentParseException.class,
                                    (ArgumentParseException) throwable, (c, e) ->
                                            commandSender.sendMessage(
                                                    "Invalid Command Argument: "
                                                            + finalThrowable.getCause().getMessage())
                            );
                        } else if (throwable instanceof CommandExecutionException) {
                            this.manager.handleException(sender,
                                    CommandExecutionException.class,
                                    (CommandExecutionException) throwable, (c, e) -> {
                                        commandSender.sendMessage(MESSAGE_INTERNAL_ERROR);
                                        MinecraftServer.LOGGER.error(
                                                "Exception executing command handler",
                                                finalThrowable.getCause()
                                        );
                                    }
                            );
                        } else {
                            commandSender.sendMessage(MESSAGE_INTERNAL_ERROR);
                            MinecraftServer.LOGGER.error(
                                    "An unhandled exception was thrown during command execution",
                                    throwable
                            );
                        }
                    }
                });

    }

    @Nullable
    @Override
    public String[] onDynamicWrite(@NotNull CommandSender sender, @NotNull String text) {
        return manager.suggest(manager.mapCommandSender(sender), removeSlashPrefix(text)).toArray(String[]::new);
    }

}
