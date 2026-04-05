package com.whobuscusyt.forgediscord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.network.chat.Component;
import net.dv8tion.jda.api.OnlineStatus;

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
        if (jda != null) return true; // 👈 prevents duplicate init

        try {
            jda = JDABuilder.createDefault(token)
                    .addEventListeners(new DiscordListener())
                    .build();

            jda.awaitReady();

            return true;
        } catch (Exception e) {
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
            case "IDLE" -> OnlineStatus.IDLE;
            case "DND" -> OnlineStatus.DO_NOT_DISTURB;
            case "INVISIBLE" -> OnlineStatus.INVISIBLE;
            default -> OnlineStatus.ONLINE;
        };
    }
    public static void sendMessage(String message) {
        if (jda == null) return;

        String channelId = Config.CHANNEL_ID.get();

        if (channelId == null || channelId.isEmpty()) return;

        var channel = jda.getTextChannelById(channelId);

        if (channel != null) {
            channel.sendMessage(message).queue();
        } else {
            System.out.println("[ForgeDiscord] Invalid channel ID!");
        }
    }
    public static class DiscordListener extends ListenerAdapter {

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event.getAuthor().isBot()) return;

            String channelId = Config.CHANNEL_ID.get();
            if (!event.getChannel().getId().equals(channelId)) return;

            String name = event.getAuthor().getName();
            String message = event.getMessage().getContentRaw();


            if (message == null || message.isBlank()) return;

            sendToMinecraft("§b[§b§lDISCORD§r§b] §f" + name + ": §r" + message);
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
}