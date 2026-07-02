package com.whobuscusyt.forgediscord;

import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import com.whobuscusyt.forgediscord.Discord.DiscordManager;
import com.whobuscusyt.forgediscord.Discord.DiscordCommand;
import java.awt.*;
import java.io.File;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod("forgediscord")
public class ForgeDiscord {

    public ForgeDiscord() {
        File folder = new File("config/ForgeDiscord");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        SyncConfig.load();
        LinkManager.load();
        AdminManager.load();
        org.apache.logging.log4j.core.Logger logger =
                (org.apache.logging.log4j.core.Logger)
                        org.apache.logging.log4j.LogManager
                                .getRootLogger();

        DiscordConsoleAppender appender =
                (DiscordConsoleAppender)
                        DiscordConsoleAppender.create();

        appender.start();

        logger.addAppender(appender);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigLoad);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static final String VERSION = "1.0.3";

    private static long lastSync =
            0;

    public void onConfigLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != Config.SPEC) return;
        if (FMLEnvironment.dist == Dist.CLIENT) return;

        String token = Config.DISCORD_TOKEN.get();

        if (token == null || token.isBlank() || token.equals("PUT_TOKEN_HERE")) {
            System.out.println("[ForgeDiscord] Discord token is not configured; bot connection skipped.");
            return;
        }

        boolean connected = DiscordManager.connect(token);

        if (connected) {
            System.out.println("[ForgeDiscord] Discord connection started.");
        } else {
            System.out.println("[ForgeDiscord] Couldn't start the Discord connection. Check the configured bot token.");
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DiscordCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!DiscordManager.isConnected()) return;

        ServerPlayer player = (ServerPlayer) event.getEntity();

        String name = player.getName().getString();

        File playerFile =
                new File(

                        player.server
                                .getWorldPath(
                                        LevelResource.PLAYER_DATA_DIR
                                )
                                .toFile(),

                        player.getUUID() + ".dat"
                );

        boolean firstJoin =
                !playerFile.exists();

        if (firstJoin)
        {
         DiscordManager.sendMessage("🟢 **" + name + "** has joined the server for the first time");
        }
        else
        {
            DiscordManager.sendMessage("🟢 **" + name + "** joined the server");
        }

        if (SyncConfig.nicknameSync) {

            String discordId =
                    LinkManager.getDiscordId(
                            player.getUUID().toString()
                    );

            if (discordId != null) {

                DiscordManager.syncNickname(
                        player.getName().getString(),
                        discordId
                );
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        if (!DiscordManager.isConnected()) return;

        String name = event.getEntity().getName().getString();

        Boolean webhooks = Config.USE_WEBHOOKS.get();

            DiscordManager.sendMessage("🔴 **" + name + "** left the server");
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();

        String name = player.getName().getString();
        String message = event.getMessage().getString();
        String uuid = String.valueOf(player.getUUID());

        boolean isDev = PermissionUtil.DEV_USERS.contains(name);
        boolean isAdmin = AdminManager.isAdmin(player.getUUID());
        boolean isLinked = LinkManager.isLinked(uuid);

        String prefix = "";

        if (isLinked && isDev) {
            prefix = "[LINKED] [FD DEV] ";
        }
        else if (isLinked && isAdmin) {
            prefix = "[LINKED] [FD ADMIN] ";
        }
        else if (isDev) {
            prefix = "[ForgeDiscord DEV] ";
        }
        else if (isAdmin) {
            prefix = "[ForgeDiscord ADMIN] ";
        }
        else if (isLinked) {
            prefix = "[LINKED] ";
        }

        Boolean webhooks = Config.USE_WEBHOOKS.get();

        if (!webhooks)
        {

            DiscordManager.sendMessage(
                    prefix +
                            player.getName().getString()
                            + ": "
                            + message
            );
        }
        else
        {

            WebhookManager.sendPlayerMessage(
                    prefix,
                    player.getName().getString(),
                    player.getUUID().toString(),
                    message
            );
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!DiscordManager.isConnected()) return;

        String deathMessage = player.getCombatTracker().getDeathMessage().getString();

        Boolean webhooks = Config.USE_WEBHOOKS.get();

            DiscordManager.sendMessage("" + deathMessage);
    }
    @SubscribeEvent
    public void onAdvancement(AdvancementEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!DiscordManager.isConnected()) return;

        var advancement = event.getAdvancement();
        if (advancement.getDisplay() == null) return;

        var display = advancement.getDisplay();
        if (!display.shouldShowToast()) return;

        String title = display.getTitle().getString();
        String name = player.getName().getString();

        Boolean webhooks = Config.USE_WEBHOOKS.get();

            DiscordManager.sendMessage("**" + name + "** has made the advancement: " + title);
    }
    @SubscribeEvent
    public void onServerStart(ServerStartedEvent event) {
        AdminManager.load();
        LinkManager.load();
        SyncConfig.load();
        DiscordManager.startMonitoring();

        if (!DiscordManager.isConnected()) return;

        if (Config.USE_WEBHOOKS.get()) {

            WebhookManager.init();
        }

        DiscordManager.sendMessage("🟢 **Server has started.**");

        new Thread(() -> {
            String latest = UpdateChecker.getLatestVersion();

            if (latest == null) return;

            if (!latest.equals(VERSION)) {
                notifyAdmins(event.getServer(), latest);
            }
        }).start();
    }
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (DiscordManager.isConnected()) {
            try {
                DiscordManager.sendMessage("🔴 Server has stopped");
            } catch (Exception ignored) {}
        }

        DiscordManager.shutdown();
    }

    public void notifyAdmins(MinecraftServer server, String latestVersion) {
        String msg = "§b[§b§lForgeDiscord§r§b] Update available! Current: "
                + VERSION + " Latest: " + latestVersion;
        DiscordManager.sendMessage("⚠️ New update available: " + latestVersion);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {

            boolean isOp = server.getPlayerList().isOp(player.getGameProfile());

            boolean hasPerm = player.hasPermissions(2);

            if (isOp || hasPerm) {
                player.sendSystemMessage(Component.literal(msg));
            }
        }
    }

    private static void runConsoleCommand(String cmd) {
        try {
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();

            if (server == null) return;

            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack(),
                    cmd
            );

            DiscordManager.sendConsole("[EXECUTED] " + cmd);

        } catch (Exception e) {
            DiscordManager.sendConsole("[ERROR] " + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onServerTick(
            TickEvent.ServerTickEvent event
    ) {

        if (event.phase !=
                TickEvent.Phase.END)
            return;

        long now =
                System.currentTimeMillis();

        if (now - lastSync <
                (SyncConfig.syncDelay * 1000L))
            return;

        lastSync = now;

        syncAllPlayers();
    }

    private void syncAllPlayers() {

        if (!DiscordManager.isConnected())
            return;

        MinecraftServer server =
                ServerLifecycleHooks
                        .getCurrentServer();

        if (server == null)
            return;

        for (ServerPlayer player
                : server.getPlayerList()
                .getPlayers()) {

            String discordId =
                    LinkManager.getDiscordId(
                            player.getUUID()
                                    .toString()
                    );

            if (discordId == null)
                continue;

            DiscordManager.syncNickname(
                    player.getName().getString(),
                    discordId
            );

            DiscordManager.syncRoles(
                    player,
                    discordId
            );
        }
    }
}
