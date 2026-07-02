package com.whobuscusyt.forgediscord.Discord;

import com.mojang.brigadier.CommandDispatcher;
import com.whobuscusyt.forgediscord.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.arguments.EntityArgument;
import java.lang.String;

public class DiscordCommand {

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {

        registerCommand(
                dispatcher,
                "discord"
        );

        registerCommand(
                dispatcher,
                "forgediscord"
        );
    }

    private static void registerCommand(
            CommandDispatcher<CommandSourceStack> dispatcher,
            String name
    ) {

        dispatcher.register(

                Commands.literal(name)
                        .executes(context -> {
                            context.getSource()
                                    .sendSystemMessage(

                                            Component.literal(

                                                    "§9§lForgeDiscord\n"
                                                            + "§7Version: " + ForgeDiscord.VERSION + "\n\n"
                                                            + "§bDiscord Invite: " + Config.DISCORD_INVITE.get() + "\n\n"
                                                            + "§b/discord help"
                                            )
                                    );

                            return 1;
                        })

                        .then(Commands.literal("help")

                                .executes(context -> {

                                    boolean isAdmin =
                                            PermissionUtil.hasPermission(context.getSource());

                                    String status =
                                            isAdmin
                                                    ? "§4Admin"
                                                    : "§2Not An Admin";

                                    String commands =
                                            isAdmin
                                                    ?

                                                    "§b/discord reload\n"
                                                            + "§b/discord unlink\n"
                                                            + "§b/discord admin\n"
                                                            + "§b/discord config\n"
                                                            + "§b/discord console\n"
                                                            + "§b/discord servstats"
                                                    :

                                                    "§b/discord link\n";

                                    context.getSource()
                                            .sendSystemMessage(

                                                    Component.literal(

                                                            centerText("§9§lForgeDiscord\n")

                                                                    + "§bForgeDiscord is a Forge 1.20.1 mod that allows you to easily connect your Discord bot to your Minecraft server to monitor things like chat, leaves, joins, deaths, etc.\n\n"

                                                                    + "§b§lCurrent Mod Version: §r§bv" + ForgeDiscord.VERSION + "\n\n"

                                                                    + "§b§lPermission Status: " + "§r" + status + "\n\n"

                                                                    + "§b§lCOMMANDS:\n\n"

                                                                    + commands
                                                    )
                                            );

                                    return 1;
                                }))

                        .then(Commands.literal("invite")
                                .executes(context -> {
                                    context.getSource().sendSystemMessage(
                                            Component.literal(
                                                    Config.MOD_PREFIX.get() + " §bDiscord Invite: §a" + Config.DISCORD_INVITE.get()
                                            )
                                    );
                                    return 1;
                                }))

                        .then(Commands.literal("bot")
                                .requires(source ->
                                        source.hasPermission(2) ||
                                                isDev(source) ||
                                                isForgeDiscordAdmin(source)
                                )
                                .executes(ctx -> {
                                    if (DiscordManager.isConnected()) {
                                        String botName = DiscordManager.getBotName();
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal(Config.MOD_PREFIX.get() + "§b Connected to: " + botName), false);
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal(Config.MOD_PREFIX.get() + "§4 Couldn't find any bot with that token, make sure the token is right."));
                                    }
                                    return 1;
                                })
                        )

                        .then(Commands.literal("reload")
                                .requires(source ->
                                        source.hasPermission(2) ||
                                                isDev(source) ||
                                                isForgeDiscordAdmin(source)
                                )
                                .executes(ctx -> {
                                    DiscordManager.reload();

                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal(Config.MOD_PREFIX.get() + " §bMod Reloaded!"), false);

                                    return 1;
                                })
                        )
                        .then(Commands.literal("servstats")
                                .requires(source ->
                                        source.hasPermission(2) ||
                                                isDev(source) ||
                                                isForgeDiscordAdmin(source)
                                )
                                .executes(ctx -> {
                                    sendServerStats(ctx.getSource());
                                    return 1;
                                })
                        )
                        .then(Commands.literal("test")
                                .requires(source ->
                                        source.hasPermission(2) ||
                                                isDev(source) ||
                                                isForgeDiscordAdmin(source)
                                )
                                .then(Commands.literal("join")
                                        .executes(ctx -> {
                                            DiscordManager.sendMessage("🟢 **ForgeDiscord** joined the server!");
                                            return 1;
                                        })
                                )

                                .then(Commands.literal("leave")
                                        .executes(ctx -> {
                                            DiscordManager.sendMessage("🔴 **ForgeDiscord** left the server!");
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("alert")
                                        .executes(context -> {
                                            context.getSource()
                                             .sendSystemMessage(
                                                     Component.literal(Config.MOD_PREFIX.get() + "§4 WARNING: This could crash your server if you spike RAM during this process. to confirm this action, do /discord test alert confirm")
                                             );
                                            return 1;
                                        })
                                        .then(Commands.literal("confirm")
                                                .executes(ctx -> {
                                                    spikeRamForTest();
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("console")
                                .requires(source ->
                                        source.hasPermission(2) ||
                                                isDev(source) ||
                                                isForgeDiscordAdmin(source)
                                )

                                .then(Commands.argument("cmd", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String cmd = StringArgumentType.getString(context, "cmd");

                                            CommandSourceStack source =
                                                    context.getSource();

                                            runConsoleCommand(context.getSource(), cmd);

                                            for (ServerPlayer player
                                                    : source.getServer()
                                                    .getPlayerList()
                                                    .getPlayers()) {

                                                player.sendSystemMessage(
                                                        Component.literal(
                                                                Config.MOD_PREFIX.get() + "§b Executed console command: §a" + cmd
                                                        )
                                                );
                                            }

                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("admin")
                                .requires(source ->
                                        source.hasPermission(2) ||
                                                isDev(source)
                                )
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

                                                    if (AdminManager.isAdmin(target.getUUID())) {
                                                        ctx.getSource().sendFailure(Component.literal(Config.MOD_PREFIX.get() + " §b" + target.getName().getString() + " couldn't be added as an admin!"));
                                                        return 0;
                                                    }

                                                    AdminManager.add(target.getUUID());

                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal(Config.MOD_PREFIX.get() + " §a" + target.getName().getString() + " was successfully added as an admin!"), false);

                                                    return 1;
                                                })
                                        )
                                )

                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

                                                    if (!AdminManager.isAdmin(target.getUUID())) {
                                                        ctx.getSource().sendFailure(Component.literal(Config.MOD_PREFIX.get() + " §b" + target.getName().getString() + " doesn't appear to be an admin!"));
                                                        return 0;
                                                    }

                                                    AdminManager.remove(target.getUUID());

                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal(Config.MOD_PREFIX.get() + " §aYou have successfully removed " + target.getName().getString() + "!"), false);

                                                    return 1;
                                                })
                                        )
                                )

                                .then(Commands.literal("list")
                                        .executes(ctx -> {
                                            sendAdminList(ctx.getSource());
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("config")
                                .requires(source ->
                                        PermissionUtil.hasPermission(source)
                                )
                                .then(Commands.literal("status")
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String value = StringArgumentType.getString(ctx, "value");
                                                    Config.STATUS.set(value);
                                                    DiscordManager.reload();
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal(Config.MOD_PREFIX.get() + " §bStatus set to §a" + value),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("channel")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String id = StringArgumentType.getString(ctx, "id");
                                                    Config.CHANNEL_ID.set(id);
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal(Config.MOD_PREFIX.get() + " §bChat channel set to §a" + id),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("console-channel")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String id = StringArgumentType.getString(ctx, "id");
                                                    Config.CONSOLE_ID.set(id);
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal(Config.MOD_PREFIX.get() + " §bConsole channel set to §a" + id),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("activity-type")
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String type = StringArgumentType.getString(ctx, "type");
                                                    Config.ACTIVITY_TYPE.set(type);
                                                    DiscordManager.reload();
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal(Config.MOD_PREFIX.get() + " §bActivity type set to §a" + type),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("activity-text")
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String activityText = StringArgumentType.getString(ctx, "text");
                                                    Config.ACTIVITY_TEXT.set(activityText);
                                                    DiscordManager.reload();
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal(Config.MOD_PREFIX.get() + " §bActivity text set to §a" + activityText),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("invite")
                                        .then(Commands.argument("url", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String url = StringArgumentType.getString(ctx, "url");
                                                    Config.DISCORD_INVITE.set(url);
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal(Config.MOD_PREFIX.get() + " §bInvite set to §a" + url),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("link")
                                .executes(context -> {

                                    CommandSourceStack source =
                                            context.getSource();

                                    if (!(source.getEntity() instanceof ServerPlayer player)) {

                                        source.sendSystemMessage(
                                                Component.literal(
                                                        "§cPlayers only."
                                                )
                                        );

                                        return 0;
                                    }

                                    if (LinkManager.isLinked(
                                            player.getUUID().toString()
                                    )) {

                                        source.sendSystemMessage(
                                                Component.literal(
                                                        Config.MOD_PREFIX.get() + "§4 Your account is already linked."
                                                )
                                        );

                                        return 0;
                                    }

                                    String code =
                                            LinkCodeManager.createCode(
                                                    player.getUUID().toString(),
                                                    player.getName().getString()
                                            );

                                    source.sendSystemMessage(
                                            Component.literal(
                                                    Config.MOD_PREFIX.get() + "§b DM the code §a" + code + "§b to §o§a" + DiscordManager.getBotName() + "§r§b. It expires in 10 minutes."
                                            )
                                    );

                                    return 1;
                                })
                        )

                        .then(Commands.literal("unlink")

                                .requires(source ->
                                        PermissionUtil.hasPermission(source)
                                )

                                .then(Commands.argument(
                                                "target",
                                                StringArgumentType.greedyString()
                                        )

                                        .executes(context -> {

                                            String target =
                                                    StringArgumentType.getString(
                                                            context,
                                                            "target"
                                                    );

                                            if (target.matches("\\d+")) {

                                                if (!LinkManager.isDiscordLinked(target)) {
                                                    context.getSource().sendFailure(
                                                            Component.literal(
                                                                    Config.MOD_PREFIX.get() + "§c No linked account was found for Discord ID §a" + target + "§c."
                                                            )
                                                    );
                                                    return 0;
                                                }

                                                DiscordManager.clearSync(target);
                                                LinkManager.unlinkDiscord(target);

                                            } else {

                                                String discordId =
                                                        LinkManager.getDiscordIdByUsername(target);

                                                if (discordId == null) {
                                                    context.getSource().sendFailure(
                                                            Component.literal(
                                                                    Config.MOD_PREFIX.get() + "§c No linked account was found for §a" + target + "§c."
                                                            )
                                                    );
                                                    return 0;
                                                }

                                                DiscordManager.clearSync(discordId);
                                                LinkManager.unlinkUsername(target);
                                            }

                                            context.getSource()
                                                    .sendSystemMessage(
                                                            Component.literal(
                                                                    Config.MOD_PREFIX.get() + "§b Unlinked " + "§a" + target + "§b!"
                                                            )
                                                    );

                                            return 1;
                                        }))
                        )
                        .then(Commands.argument("subcommand", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String input = StringArgumentType.getString(context, "subcommand");
                                    String command = input.split("\\s+", 2)[0].toLowerCase();

                                    boolean protectedCommand = switch (command) {
                                        case "bot", "reload", "servstats", "test", "console",
                                             "admin", "config", "unlink" -> true;
                                        default -> false;
                                    };

                                    if (protectedCommand && !PermissionUtil.hasPermission(context.getSource())) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        Config.NO_PERMISSION.get()
                                                                .replace("%Prefix%", Config.MOD_PREFIX.get())
                                                )
                                        );
                                    } else {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        Config.MOD_PREFIX.get() + " §cUnknown subcommand. Try §b/discord help§c."
                                                )
                                        );
                                    }
                                    return 0;
                                }))
        );
    }


    private static void sendServerStats(CommandSourceStack source) {

        long maxRam =
                Runtime.getRuntime()
                        .maxMemory();

        long usedRam =
                Runtime.getRuntime()
                        .totalMemory()

                        - Runtime.getRuntime()
                        .freeMemory();

        int ramPercent =
                (int) ((usedRam * 100) / maxRam);

        double cpuLoad =
                getCpuLoad();

        int cpuPercent =
                (int) (cpuLoad * 100);

        long usedRamMB =
                usedRam / (1024 * 1024);

        long maxRamMB =
                maxRam / (1024 * 1024);

        String usedRamStr =
                formatMemory(usedRamMB);

        String maxRamStr =
                formatMemory(maxRamMB);

        String ramColor =
                getColor(ramPercent);

        String cpuColor =
                getColor(cpuPercent);

        source.sendSuccess(() -> Component.literal(

                ramColor
                        + "RAM: "
                        + usedRamStr
                        + "/"
                        + maxRamStr
                        + " ("
                        + ramPercent
                        + "%)"

        ), false);

        source.sendSuccess(() -> Component.literal(

                cpuColor
                        + "CPU: "
                        + cpuPercent
                        + "%/100%"

        ), false);
    }

    private static String formatMemory(
            long mb
    ) {

        if (mb >= 1024) {

            return String.format(
                    "%.2fGB",
                    mb / 1024.0
            );
        }

        return mb + "MB";
    }

    private static String getColor(int percent) {
        if (percent >= 85) return "§4";
        if (percent >= 75) return "§c";
        if (percent >= 50) return "§6";
        if (percent <= 25) return "§a";
        return "§f";
    }


    private static double getCpuLoad() {
        try {
            OperatingSystemMXBean osBean =
                    (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

            double load = osBean.getCpuLoad();

            if (load < 0) return 0; // fallback

            return load;
        } catch (Exception e) {
            return 0;
        }
    }

    public static void spikeRamForTest() {
        DiscordManager.sendAlert(91);
        System.out.println("[ForgeDiscord] Sent simulated high-RAM alert.");
    }

    private static boolean isDev(CommandSourceStack source) {
        try {
            String name = source.getPlayerOrException().getName().getString();
            return name.equalsIgnoreCase("WhoBuscusYT");
        } catch (Exception e) {
            return false;
        }
    }

    private static void runConsoleCommand(CommandSourceStack source, String command) {

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) return;

        server.execute(() -> {
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack(),
                    command
            );
        });

        boolean log = server.getGameRules().getBoolean(
                net.minecraft.world.level.GameRules.RULE_LOGADMINCOMMANDS
        );

        if (log) {
            System.out.println("[ForgeDiscord] Executed Console Command \"" + command + "\"");
        }
    }
    private static void sendAdminList(CommandSourceStack source) {
        MinecraftServer server = source.getServer();

        source.sendSuccess(() -> Component.literal("§4§lForgeDiscord Developer"), false);
        source.sendSuccess(() -> Component.literal("§4§lWhoBuscusYT"), false);
        source.sendSuccess(() -> Component.literal(" "), false);

        source.sendSuccess(() -> Component.literal("§b§lSERVER ADMINS (operator/manual)"), false);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {

            String name = player.getName().getString();

            boolean isOp = server.getPlayerList().isOp(player.getGameProfile());
            boolean isManual = AdminManager.isAdmin(player.getUUID());

            if (isOp) {
                source.sendSuccess(() ->
                        Component.literal("§b§l" + name + " §4§l[operator]"), false);
            } else if (isManual) {
                source.sendSuccess(() ->
                        Component.literal("§b§l" + name + " §c§l[manual]"), false);
            }
        }
    }
    private static void OtherDisc(CommandSourceStack source) {
        MinecraftServer server = source.getServer();

        source.sendSuccess(() -> Component.literal("§eFabricDiscord"), false);
        source.sendSuccess(() -> Component.literal("§6NeoDiscord"), false);
    }
    private static boolean isForgeDiscordAdmin(CommandSourceStack source) {
        try {
            return AdminManager.isAdmin(
                    source.getPlayerOrException().getUUID()
            );
        } catch (Exception e) {
            return false;
        }
    }

    public static String centerText(
            String text
    ) {

        int width = 50;

        int spaces =
                (width - text.length()) / 2;

        return " ".repeat(
                Math.max(0, spaces)
        ) + text;
    }
}
