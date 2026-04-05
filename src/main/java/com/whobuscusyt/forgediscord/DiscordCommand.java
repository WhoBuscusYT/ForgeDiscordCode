package com.whobuscusyt.forgediscord;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class DiscordCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("discord")

                        .then(Commands.literal("bot")
                                .executes(ctx -> {
                                    if (DiscordManager.isConnected()) {
                                        String name = DiscordManager.getBotName();
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("Connected to:" + name), false);
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal("Couldn't find any bot with that token, make sure the token is right."));
                                    }
                                    return 1;
                                })
                        )

                        .then(Commands.literal("reload")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> {
                                    DiscordManager.reloadPresence();

                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("Discord reloaded!"), false);

                                    return 1;
                                })
                        )
        );
    }
}