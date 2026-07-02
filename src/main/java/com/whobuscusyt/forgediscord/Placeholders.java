package com.whobuscusyt.forgediscord;

import net.dv8tion.jda.api.entities.Member;

public class Placeholders {

    public static String apply(
            String format,
            String mcUsername,
            Member member
    ) {

        String displayName =
                member.getEffectiveName();

        String discordUsername =
                member.getUser().getName();


        return format

                .replace(
                        "%username%",
                        mcUsername
                )

                .replace(
                        "%Prefix%",
                        Config.MOD_PREFIX.get()
                )

                .replace(
                        "%displayname%",
                        displayName
                )

                .replace(
                        "%discusername%",
                        discordUsername
                );
    }
}
