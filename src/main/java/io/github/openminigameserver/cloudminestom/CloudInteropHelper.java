package io.github.openminigameserver.cloudminestom;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.StaticArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentDynamicWord;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.command.builder.arguments.minecraft.SuggestionType;
import org.jetbrains.annotations.NotNull;

final class CloudInteropHelper {

    @NotNull
    protected static String removeSlashPrefix(@NotNull String text) {
        String command = text;
        if (command.startsWith("/"))
            command = command.substring(1);
        return command;
    }

    protected static <C> Argument<?> convertCloudArgumentToMinestom(CommandArgument<C, ?> arg) {
        Argument<?> result;
        if (arg instanceof StaticArgument) {
            result =
                    new ArgumentWord(arg.getName()).from(((StaticArgument<?>) arg).getAliases().toArray(String[]::new));
        } else {
            var parser = arg.getParser();
            if (arg instanceof StringArgument && ((StringArgument<?>) arg).getStringMode() == StringArgument.StringMode.GREEDY) {
                result = new ArgumentStringArray((arg.getName()));
            } else if (parser instanceof StringArgument.StringParser && ((StringArgument.StringParser<?>) parser).getStringMode() == StringArgument.StringMode.GREEDY) {
                result = new ArgumentStringArray((arg.getName()));
            } else {
                result = new ArgumentDynamicWord(arg.getName(), SuggestionType.ASK_SERVER);
            }
        }
        return result;
    }
}
