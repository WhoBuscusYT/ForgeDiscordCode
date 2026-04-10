package com.whobuscusyt.forgediscord;

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
import com.whobuscusyt.forgediscord.PermissionUtil;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import com.whobuscusyt.forgediscord.Discord.DiscordManager;
import com.whobuscusyt.forgediscord.Discord.DiscordCommand;
import com.whobuscusyt.forgediscord.Config;
import com.whobuscusyt.forgediscord.AdminManager;

@Mod("forgediscord")
public class ForgeDiscord {

    public ForgeDiscord() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigLoad);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static final String VERSION = "1.0.1";

    public void onConfigLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != Config.SPEC) return;
        if (FMLEnvironment.dist == Dist.CLIENT) return;

        String token = Config.DISCORD_TOKEN.get();

        boolean connected = DiscordManager.connect(token);

        if (connected) {
            System.out.println("[ForgeDiscord] Connected to the bot!");
        } else {
            System.out.println("[ForgeDiscord] Couldn't connect to the bot.");
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DiscordCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();

        boolean isAdmin = AdminManager.isAdmin(player.getUUID());

        String name = player.getName().getString();

        try {
            DiscordManager.sendMessage("🟢 **" + name + "** joined the server");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        if (!DiscordManager.isConnected()) return;

        String name = event.getEntity().getName().getString();

        DiscordManager.sendMessage("🔴 **" + name + "** left the server");
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();

        String name = player.getName().getString();
        String message = event.getMessage().getString();

        boolean isDev = PermissionUtil.DEV_USERS.contains(name);
        boolean isAdmin = AdminManager.isAdmin(player.getUUID());

        if (isDev) {
            DiscordManager.sendMessage("**[ForgeDiscord DEV] " + name + "**: " + message);
        } else if (isAdmin) {
            DiscordManager.sendMessage("**[ForgeDiscord ADMIN] " + name + "**: " + message);
        } else {
            DiscordManager.sendMessage("**" + name + "**: " + message);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!DiscordManager.isConnected()) return;

        String name = player.getName().getString();

        DiscordManager.sendMessage("**" + name + "** died");
    }
    @SubscribeEvent
    public void onAdvancement(AdvancementEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!DiscordManager.isConnected()) return;

        var advancement = event.getAdvancement();
        if (advancement.getDisplay() == null) return;

        var display = advancement.getDisplay();
        if (!display.shouldShowToast()) return;

        String title = display.getTitle().getString();
        String name = player.getName().getString();

        DiscordManager.sendMessage("**" + name + "** has made the advancement: " + title);
    }
    @SubscribeEvent
    public void onServerStart(ServerStartedEvent event) {
        if (!DiscordManager.isConnected()) return;
        AdminManager.load();

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
        if (!DiscordManager.isConnected()) return;

        DiscordManager.sendMessage("🔴 **Server has stopped.**");

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
}
