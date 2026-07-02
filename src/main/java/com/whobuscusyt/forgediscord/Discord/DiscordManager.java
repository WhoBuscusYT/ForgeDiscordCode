package com.whobuscusyt.forgediscord.Discord;

import com.sun.management.OperatingSystemMXBean;
import com.whobuscusyt.forgediscord.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import java.util.List;
import java.util.ArrayList;
import net.dv8tion.jda.api.entities.Role;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;

public class DiscordManager {

    private static JDA jda;

    private static final List<String>
            consoleBuffer =
            Collections.synchronizedList(new ArrayList<>());

    private static volatile boolean
            sendingConsole =
            false;

    private static ScheduledExecutorService monitor;
    private static long lastAlertTime;

    public static boolean isConnected() {
        return jda != null;
    }

    public static String getBotName() {
        if (jda == null) return "Not connected";
        return jda.getSelfUser().getName();
    }

    public static boolean connect(String token) {
        if (jda != null) {
            System.out.println("[ForgeDiscord] Already connected, skipping...");
            return true;
        }

        try {
            System.out.println(
                    "[ForgeDiscord] Registering Discord listener..."
            );
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .addEventListeners(new DiscordListener())
                    .build();

            Thread connectionThread = new Thread(() -> {
                try {
                    jda.awaitReady();

                    System.out.println("[ForgeDiscord] Bot connected!");
                    if (Config.USE_WEBHOOKS.get()) {

                        WebhookManager.init();
                    }
                    jda.getPresence().setActivity(

                            getActivity(
                                    Config.ACTIVITY_TYPE.get(),
                                    Config.ACTIVITY_TEXT.get()
                            )
                    );
                    jda.getPresence().setStatus(
                            getStatus(Config.STATUS.get())
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    if (jda != null) {
                        jda.shutdownNow();
                        jda = null;
                    }
                }
            }, "ForgeDiscord-connection");
            connectionThread.setDaemon(true);
            connectionThread.start();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void reload() {
        SyncConfig.load();
        if (jda == null) return;

        jda.getPresence().setStatus(getStatus(Config.STATUS.get()));
        jda.getPresence().setActivity(
                getActivity(Config.ACTIVITY_TYPE.get(), Config.ACTIVITY_TEXT.get())
        );

        if (Config.USE_WEBHOOKS.get()) {
            WebhookManager.init();
        } else {
            WebhookManager.shutdown();
        }
    }

    public static OnlineStatus getStatus(String status) {
        return switch (status.toUpperCase()) {
            case "ONLINE" -> OnlineStatus.ONLINE;
            case "IDLE" -> OnlineStatus.IDLE;
            case "DND" -> OnlineStatus.DO_NOT_DISTURB;
            case "DO NOT DISTURB" -> OnlineStatus.DO_NOT_DISTURB;
            case "INVISIBLE" -> OnlineStatus.INVISIBLE;
            case "INVIS" -> OnlineStatus.INVISIBLE;
            case "OFFLINE" -> OnlineStatus.INVISIBLE;
            default -> OnlineStatus.ONLINE;
        };
    }
    public static void sendMessage(String msg) {

        try {

            if (jda == null) return;

            if (jda.getStatus() == JDA.Status.SHUTDOWN) return;

            String formatted =
                    TextFormatter.minecraftToDiscord(msg);

            var channel =
                    jda.getTextChannelById(
                            Config.CHANNEL_ID.get()
                    );

            if (channel != null) {
                channel.sendMessage(formatted).queue();
            }

        } catch (Exception ignored) {
        }
    }

    public static class DiscordListener
            extends ListenerAdapter {

        @Override
        public void onMessageReceived(
                MessageReceivedEvent event
        ) {

            if (event.getAuthor().isBot()) {
                return;
            }

            // =========================
            // DM LINKING SYSTEM
            // =========================

            if (!event.isFromGuild()) {

                String msg =
                        event.getMessage()
                                .getContentRaw()
                                .trim();

                LinkCodeData data =
                        LinkCodeManager.get(msg);

                if (data == null) {

                    event.getChannel().sendMessage(
                            "That code doesn't seem right, be sure to check it."
                    ).queue();

                    return;
                }

                String discordId =
                        event.getAuthor().getId();

                if (LinkManager.isDiscordLinked(discordId)) {

                    event.getChannel().sendMessage(
                            "You're already linked to "
                                    + LinkManager.getUsernameByDiscord(discordId)
                    ).queue();

                    return;
                }

                LinkManager.link(
                        data.uuid(),
                        data.username(),
                        discordId
                );

                MinecraftServer server =
                        ServerLifecycleHooks.getCurrentServer();

                if (server != null) {
                    server.execute(() -> {
                        for (String cmd : SyncConfig.linkCommands) {
                            String parsed = cmd.replace("%player%", data.username());
                            server.getCommands().performPrefixedCommand(
                                    server.createCommandSourceStack(),
                                    parsed
                            );
                        }
                    });
                }

                DiscordManager.syncNickname(
                        data.username(),
                        discordId
                );

                LinkCodeManager.remove(msg);

                event.getChannel().sendMessage(
                        "Successfully linked to "
                                + data.username()
                                + " (" + data.uuid() + ")"
                ).queue();

                return;
            }

            String channelId =
                    event.getChannel().getId();

            if (channelId.equals(Config.CONSOLE_ID.get())) {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) return;

                String command = event.getMessage().getContentRaw().trim();
                if (command.startsWith("/")) {
                    command = command.substring(1);
                }

                if (!command.isBlank()) {
                    String finalCommand = command;
                    server.execute(() -> server.getCommands().performPrefixedCommand(
                            server.createCommandSourceStack(),
                            finalCommand
                    ));
                }
                return;
            }

            if (!channelId.equals(
                    Config.CHANNEL_ID.get()
            )) {
                return;
            }

            String author =
                    event.getAuthor().getName();

            String message =
                    event.getMessage()
                            .getContentRaw();

            for (var response : SyncConfig.autoResponses.entrySet()) {
                if (message.equalsIgnoreCase(response.getKey())) {
                    event.getChannel().sendMessage(response.getValue()).queue();
                    break;
                }
            }

            MinecraftServer server =
                    ServerLifecycleHooks.getCurrentServer();

            if (server == null) return;

            server.execute(() -> {

                String formatted =
                        "§9[Discord] §f"
                                + author
                                + ": "
                                + message;

                server.getPlayerList()
                        .broadcastSystemMessage(
                                Component.literal(formatted),
                                false
                        );
            });
        }
    }
    public static void sendToMinecraft(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal(message), false
            );
        }
    }
    public static void shutdown() {
        WebhookManager.shutdown();

        if (monitor != null) {
            monitor.shutdownNow();
            monitor = null;
        }

        if (jda == null) return;

        try {
            jda.getPresence().setStatus(OnlineStatus.OFFLINE);
            jda.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jda = null;
        }
    }
    public static void startMonitoring() {
        if (monitor != null && !monitor.isShutdown()) return;

        monitor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ForgeDiscord-monitor");
            thread.setDaemon(true);
            return thread;
        });
        monitor.scheduleAtFixedRate(DiscordManager::checkUsage, 60, 60, TimeUnit.SECONDS);
    }
    public static void sendAlert(int ram) {
        int cpu = (int) (getCpuLoad() * 100);
        sendAlert(ram, cpu);
    }

    private static void sendAlert(int ram, int cpu) {
        String ownerId = Config.OWNER_ID.get();

        String msg =
                "**High Server Usage!**\n"
                + "RAM: " + ram + "% (" + formatMemory(getUsedRam()) + "/" + formatMemory(getMaxRam()) + ")\n"
                + "CPU: " + cpu + "%\n"
                + "<@" + ownerId + ">";

        sendMessage(msg);
    }
    private static void checkUsage() {
        if (jda == null) return;

        long maxRam = Runtime.getRuntime().maxMemory();
        long usedRam = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        int ramPercent = (int) ((usedRam * 100) / maxRam);

        double cpuLoad = getCpuLoad();
        int cpuPercent = (int) (cpuLoad * 100);

        long now = System.currentTimeMillis();
        if ((ramPercent >= 90 || cpuPercent >= 90)
                && now - lastAlertTime >= TimeUnit.MINUTES.toMillis(10)) {
            lastAlertTime = now;
            sendAlert(ramPercent, cpuPercent);
        }
    }
    private static double getCpuLoad() {
        try {
            OperatingSystemMXBean osBean =
                    (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

            double load = osBean.getCpuLoad();

            if (load < 0) return 0;

            return load;
        } catch (Exception e) {
            return 0;
        }
    }

    private static Activity getActivity(String type, String text) {
        if (text == null || text.isBlank()) return null;

        return switch (type.toUpperCase()) {
            case "PLAYING" -> Activity.playing(text);
            case "WATCHING" -> Activity.watching(text);
            case "LISTENING" -> Activity.listening(text);
            case "COMPETING" -> Activity.competing(text);
            case "STREAMING" -> Activity.streaming(text, text);
            case "CUSTOM" -> Activity.customStatus(text);
            default -> Activity.playing(text);
        };
    }

    public static void sendConsole(
            String message
    ) {

        try {

            consoleBuffer.add(message);

            if (sendingConsole)
                return;

            sendingConsole = true;

            new Thread(() -> {

                try {

                    Thread.sleep(2000);

                    if (jda == null)
                        return;

                    var channel =
                            jda.getTextChannelById(
                                    Config.CONSOLE_ID.get()
                            );

                    if (channel == null)
                        return;

                    StringBuilder builder = new StringBuilder();

                    synchronized (consoleBuffer) {
                        for (String line : consoleBuffer) {
                            builder.append(line).append("\n");
                        }
                        consoleBuffer.clear();
                    }

                    String content =
                            builder.toString();

                    for (int start = 0; start < content.length(); start += 1900) {
                        String chunk = content.substring(
                                start,
                                Math.min(start + 1900, content.length())
                        );
                        channel.sendMessage("```ansi\n" + chunk + "\n```").queue();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    sendingConsole = false;
                }

            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long getUsedRam() {

        Runtime runtime =
                Runtime.getRuntime();

        return (
                runtime.totalMemory()
                        - runtime.freeMemory()
        ) / (1024 * 1024);
    }

    public static long getMaxRam() {

        return Runtime.getRuntime()
                .maxMemory()

                / (1024 * 1024);
    }

    public static int getRamPercent() {

        long used =
                getUsedRam();

        long max =
                getMaxRam();

        return (int) (
                (used * 100) / max
        );
    }

    public static String formatMemory(
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

    public static Guild getGuild() {

        if (jda == null)
            return null;

        TextChannel channel =
                jda.getTextChannelById(
                        Config.CHANNEL_ID.get()
                );

        if (channel != null) {
            return channel.getGuild();
        }

        TextChannel console =
                jda.getTextChannelById(
                        Config.CONSOLE_ID.get()
                );

        if (console != null) {
            return console.getGuild();
        }

        return null;
    }

    public static void syncNickname(
            String mcUsername,
            String discordId
    ) {

        try {


            if (!SyncConfig.nicknameSync)
                return;

            Guild guild = getGuild();

            if (guild == null)
                return;

            Member member =
                    guild.retrieveMemberById(discordId)
                            .complete();

            if (member == null)
                return;

            if (member.isOwner())
                return;

            if (member.getUser().isBot())
                return;

            String formatted =
                    Placeholders.apply(
                            SyncConfig.nicknameFormat,
                            mcUsername,
                            member
                    );

            member.modifyNickname(formatted)
                    .queue();

            System.out.println(
                    "[ForgeDiscord] Synced nickname for "
                            + mcUsername
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JDA getJDA() {
        return jda;
    }

    public static void syncRoles(
            ServerPlayer player,
            String discordId
    ) {

        try {

            if (!SyncConfig.roleSync)
                return;

            Guild guild = getGuild();

            if (guild == null)
                return;

            Member member =
                    guild.retrieveMemberById(
                            discordId
                    ).complete();

            if (member == null)
                return;

            if (member.isOwner())
                return;

            if (member.getUser().isBot())
                return;

            if (!net.minecraftforge.fml.ModList.get().isLoaded("luckperms"))
                return;

            LuckPerms api =
                    LuckPermsProvider.get();

            User user =
                    api.getUserManager()
                            .getUser(
                                    player.getUUID()
                            );

            if (user == null)
                return;

            Collection<String> groups =
                    user.getInheritedGroups(
                                    user.getQueryOptions()
                            ).stream()

                            .map(Group::getName)

                            .toList();

            Set<String> desiredRoleIds = new HashSet<>();

            for (String group : groups) {
                List<String> configuredRoles = SyncConfig.roleMap.get(group.toLowerCase());
                if (configuredRoles != null) {
                    desiredRoleIds.addAll(configuredRoles);
                }
            }

            for (String group
                    : groups) {

                List<String> roleIds =
                        SyncConfig.roleMap
                                .get(group.toLowerCase());

                System.out.println(
                        "[ForgeDiscord] Syncing roles for "
                                + player.getName().getString()
                );

                System.out.println(
                        "[ForgeDiscord] Minecraft group: "
                                + group
                );

                System.out.println(
                        "[ForgeDiscord] Adding Discord role: "
                                + roleIds
                );

                if (roleIds == null)
                    continue;

                for (String roleId
                        : roleIds) {

                    Role role =
                            guild.getRoleById(
                                    roleId
                            );

                    if (role == null)
                        continue;

                    if (!member.getRoles()
                            .contains(role)) {

                        guild.addRoleToMember(
                                member,
                                role
                        ).queue();
                    }
                }
            }

            Set<String> managedRoleIds = new HashSet<>();
            for (List<String> configuredRoles : SyncConfig.roleMap.values()) {
                if (configuredRoles != null) {
                    managedRoleIds.addAll(configuredRoles);
                }
            }

            for (Role role : member.getRoles()) {
                if (managedRoleIds.contains(role.getId())
                        && !desiredRoleIds.contains(role.getId())) {
                    guild.removeRoleFromMember(member, role).queue();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearSync(
            String discordId
    ) {
        try {
            Guild guild = getGuild();
            if (guild == null) return;

            Member member = guild.retrieveMemberById(discordId).complete();
            if (member == null || member.isOwner() || member.getUser().isBot()) return;

            if (SyncConfig.nicknameSync) {
                member.modifyNickname(null).queue();
            }

            if (SyncConfig.roleSync) {
                for (List<String> roleIds : SyncConfig.roleMap.values()) {
                    if (roleIds == null) continue;
                    for (String roleId : roleIds) {
                        Role role = guild.getRoleById(roleId);
                        if (role != null && member.getRoles().contains(role)) {
                            guild.removeRoleFromMember(member, role).queue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
