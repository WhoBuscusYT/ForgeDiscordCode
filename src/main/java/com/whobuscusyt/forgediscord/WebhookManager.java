package com.whobuscusyt.forgediscord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.entities.Webhook;
import com.whobuscusyt.forgediscord.Discord.DiscordManager;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class WebhookManager {

    public static WebhookClient client;

    public static void init() {

        System.out.println(
                "[ForgeDiscord] Initializing webhooks..."
        );

        try {

            shutdown();

            if (DiscordManager.getJDA() == null)
                return;

            TextChannel channel =
                    DiscordManager.getJDA()
                            .getTextChannelById(
                                    Config.CHANNEL_ID.get()
                            );

            if (channel == null)
            {

                System.out.println(
                        "[ForgeDiscord] Could not find channel for webhooks."
                );

                return;
            }
            else
            {
                System.out.println(
                        "[ForgeDiscord] Found channel: "
                                + channel.getName()
                );
            }

            Webhook webhook = null;

            for (Webhook hook
                    : channel.retrieveWebhooks().complete()) {

                if (hook.getName().equals(
                        "FD Webhook"
                )) {

                    webhook = hook;

                    break;
                }
            }

            if (webhook == null) {

                System.out.println(
                        "[ForgeDiscord] Creating webhook..."
                );

                webhook =
                        channel.createWebhook(
                                "FD Webhook"
                        ).complete();

                System.out.println(
                        "[ForgeDiscord] Webhook created."
                );
            }

            client =
                    WebhookClient.withUrl(
                            webhook.getUrl()
                    );

            System.out.println(
                    "[ForgeDiscord] Webhooks enabled."
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendPlayerMessage(
            String prefix,
            String username,
            String uuid,
            String message
    ) {

        String avatar =
                "https://mc-heads.net/avatar/" + uuid;

        sendWebhookMessage(
                prefix,
                prefix + username,
                avatar,
                TextFormatter.minecraftToDiscord(message)
        );
    }

    public static void sendWebhookMessage(
            String prefix,
            String username,
            String avatar,
            String message
    ) {

        try {

            if (client == null)
                return;

            WebhookMessageBuilder builder =
                    new WebhookMessageBuilder();

            String safeUsername = username == null || username.isBlank()
                    ? "Minecraft"
                    : username.substring(0, Math.min(username.length(), 80));

            builder.setUsername(safeUsername);

            builder.setAvatarUrl(avatar);

            builder.setContent(message);

            client.send(builder.build());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendEmbed(WebhookEmbed embed) {
        if (client != null && embed != null) {
            client.send(embed);
        }
    }

    public static void shutdown() {
        if (client == null) return;

        try {
            client.close();
        } catch (Exception ignored) {
        } finally {
            client = null;
        }
    }
}
