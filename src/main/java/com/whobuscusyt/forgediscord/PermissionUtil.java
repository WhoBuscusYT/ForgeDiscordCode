package com.whobuscusyt.forgediscord;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import java.util.UUID;

public class PermissionUtil {

    public static boolean hasPermission(CommandSourceStack source) {

        if (source.hasPermission(2)) return true;

        if (!(source.getEntity() instanceof ServerPlayer player)) return false;

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
}