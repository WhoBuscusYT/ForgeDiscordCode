package com.whobuscusyt.forgediscord.Discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.network.chat.Component;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import net.dv8tion.jda.api.entities.Activity;
import com.whobuscusyt.forgediscord.Config;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordManager {

    private static JDA jda;

    public static boolean isConnected() {
        return jda != null;
    }

    public static String getBotName() {
        if (jda == null) return "Not connected";
        return jda.getSelfUser().getName();
    }

    public static boolean connect(String token) {
        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new DiscordListener()) // also needed
                    .build();

            new Thread(() -> {
                try {
                    jda.awaitReady();

                    jda.getPresence().setStatus(getStatus(Config.STATUS.get()));
                    jda.getPresence().setActivity(
                            getActivity(Config.ACTIVITY_TYPE.get(), Config.ACTIVITY_TEXT.get())
                    );

                    System.out.println("[ForgeDiscord] Bot connected!");

                } catch (Exception e) {
                    System.out.println("[ForgeDiscord] Failed to connect bot:");
                    e.printStackTrace();
                }
            }).start();

            return true;

        } catch (Exception e) {
            System.out.println("[ForgeDiscord] CRITICAL ERROR (safe fallback):");
            e.printStackTrace();
            return false;
        }
    }

    public static void reloadPresence() {
        if (jda == null) return;

        String statusString = Config.STATUS.get();
        jda.getPresence().setStatus(getStatus(statusString));
    }

    public static OnlineStatus getStatus(String status) {
        return switch (status.toUpperCase()) {
            case "ONLINE" -> OnlineStatus.ONLINE;
            case "IDLE" -> OnlineStatus.IDLE;
            case "DND" -> OnlineStatus.DO_NOT_DISTURB;
            case "DO NOT DISTURB" -> OnlineStatus.DO_NOT_DISTURB;
            case "INVISIBLE" -> OnlineStatus.INVISIBLE;
            case "INVIS" -> OnlineStatus.INVISIBLE;
            default -> OnlineStatus.ONLINE;
        };
    }
    public static void sendMessage(String msg) {
        try {
            if (jda == null) return;
            if (jda.getStatus() == JDA.Status.SHUTDOWN) return;

            var channel = jda.getTextChannelById(Config.CHANNEL_ID.get());
            if (channel != null) {
                channel.sendMessage(msg).queue();
            }
        } catch (Exception ignored) {}
    }

    public static class DiscordListener extends ListenerAdapter {

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event.getAuthor().isBot()) return;

            String channelId = Config.CHANNEL_ID.get();
            if (!event.getChannel().getId().equals(channelId)) return;

            String name = event.getAuthor().getName();
            String message = event.getMessage().getContentRaw();
            String prefix = Config.MOD_PREFIX.get();


            if (message == null || message.isBlank()) return;

            sendToMinecraft( prefix + " §b§l" + name + ": §r" + message);
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
        if (jda == null) return;

        try {
            jda.getPresence().setStatus(OnlineStatus.OFFLINE);
            jda.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void startMonitoring() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // every 60s

                    checkUsage();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private static void sendAlert(int ram, int cpu) {
        String ownerId = Config.OWNER_ID.get();

        String msg =
                "⚠️ **HIGH SERVER USAGE!**\n"
                 + "RAM: " + ram + "%\n"
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

        if (ramPercent >= 90 || cpuPercent >= 90) {
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

    public static void sendConsole(String msg) {
        if (jda == null) return;

        try {
            var channel = jda.getTextChannelById(Config.CONSOLE_ID.get());
            if (channel != null) {
                channel.sendMessage("```" + msg + "```").queue();
            }
        } catch (Exception ignored) {}
    }
}