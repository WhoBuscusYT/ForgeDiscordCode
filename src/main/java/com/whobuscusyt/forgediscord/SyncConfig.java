package com.whobuscusyt.forgediscord;

import com.moandjiezana.toml.Toml;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

public class SyncConfig {

    private static final File FILE =
            new File(
                    "config/ForgeDiscord/sync.toml"
            );

    public static boolean nicknameSync =
            true;

    public static String nicknameFormat =
            "%username%";

    public static boolean roleSync =
            true;

    public static int syncDelay =
            200;

    public static final Map<String, List<String>> roleMap =
            new HashMap<>();

    public static final List<String> linkCommands =
            new ArrayList<>();

    public static final Map<String, String> autoResponses =
            new HashMap<>();

    public static void load() {

        try {

            if (!FILE.exists()) {

                System.out.println(
                        "[ForgeDiscord] sync.toml not found, creating default config..."
                );

                createDefault();
            }

            Toml toml =
                    new Toml().read(FILE);

            nicknameSync =
                    toml.getBoolean(
                            "nickname-sync",
                            true
                    );

            nicknameFormat =
                    toml.getString(
                            "nickname-format",
                            "%username%"
                    );

            roleSync =
                    toml.getBoolean(
                            "sync-roles",
                            true
                    );

            syncDelay =
                    toml.getLong(
                            "sync-delay",
                            200L
                    ).intValue();

            System.out.println(
                    "[ForgeDiscord] Nickname Sync: "
                            + nicknameSync
            );

            System.out.println(
                    "[ForgeDiscord] Role Sync: "
                            + roleSync
            );

            System.out.println(
                    "[ForgeDiscord] Sync Delay: "
                            + syncDelay
            );

            roleMap.clear();

            Toml roles =
                    toml.getTable("roles");

            if (roles != null) {

                for (String key
                        : roles.toMap().keySet()) {

                    if (key.equals("link-commands")) {
                        continue;
                    }

                    List<String> ids =
                            roles.getList(key);

                    if (ids == null) {
                        continue;
                    }

                    roleMap.put(
                            key.toLowerCase(),
                            ids
                    );

                    System.out.println(
                            "[ForgeDiscord] Loaded role group: "
                                    + key
                                    + " -> "
                                    + ids
                    );
                }
            }

            linkCommands.clear();

            List<String> commands =
                    toml.getList(
                            "link-commands"
                    );

            // Older generated configs accidentally placed this key inside [roles].
            if (commands == null && roles != null) {
                commands = roles.getList("link-commands");
            }

            if (commands != null) {

                linkCommands.addAll(commands);

                System.out.println(
                        "[ForgeDiscord] Loaded "
                                + commands.size()
                                + " link commands."
                );
            }

            autoResponses.clear();

            Toml responses =
                    toml.getTable(
                            "auto-responses"
                    );

            if (responses != null) {

                for (Map.Entry<String, Object> entry
                        : responses.toMap().entrySet()) {

                    autoResponses.put(
                            entry.getKey(),
                            String.valueOf(
                                    entry.getValue()
                            )
                    );

                    System.out.println(
                            "[ForgeDiscord] Loaded auto response: "
                                    + entry.getKey()
                                    + " -> "
                                    + entry.getValue()
                    );
                }
            }

            System.out.println(
                    "[ForgeDiscord] sync.toml loaded successfully."
            );

        } catch (Exception e) {

            System.out.println(
                    "[ForgeDiscord] Failed to load sync.toml!"
            );

            e.printStackTrace();
        }
    }

    private static void createDefault() {

        try {

            FILE.getParentFile().mkdirs();

            FileWriter writer =
                    new FileWriter(FILE);

            writer.write("""

nickname-sync=true
nickname-format="%username%"

sync-roles=true
sync-delay=200

link-commands=[
    "say %player% linked their Discord account!"
]

[roles]

default=[
    "123456789012345678"
]

admin=[
    "987654321098765432"
]

[auto-responses]

"!ip"="play.example.com"
"!help"="Make a ticket in #support!"

""");

            writer.close();

            System.out.println(
                    "[ForgeDiscord] Created default sync.toml"
            );

        } catch (Exception e) {

            System.out.println(
                    "[ForgeDiscord] Failed to create sync.toml!"
            );

            e.printStackTrace();
        }
    }
}
