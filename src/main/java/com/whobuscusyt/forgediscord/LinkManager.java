package com.whobuscusyt.forgediscord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LinkManager {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static final File FILE =
            new File(
                    "config/ForgeDiscord/linked_accounts.json"
            );

    private static Map<String, LinkData> links =
            new ConcurrentHashMap<>();

    public static String getDiscordId(
            String uuid
    ) {

        LinkData data =
                links.get(uuid);

        if (data == null)
            return null;

        return data.discordId();
    }

    public static void load() {

        try {

            if (!FILE.exists()) {

                FILE.getParentFile().mkdirs();

                if (!FILE.exists()) {

                    FILE.createNewFile();
                }

                save();

                return;
            }

            FileReader reader =
                    new FileReader(FILE);

            Type type =
                    new TypeToken<HashMap<String, LinkData>>(){}.getType();

            Map<String, LinkData> loaded = GSON.fromJson(reader, type);

            reader.close();

            links = loaded == null
                    ? new ConcurrentHashMap<>()
                    : new ConcurrentHashMap<>(loaded);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {

        try {

            FILE.getParentFile().mkdirs();

            if (!FILE.exists()) {

                FILE.createNewFile();
            }

            FileWriter writer =
                    new FileWriter(FILE);

            GSON.toJson(
                    links,
                    writer
            );

            writer.close();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public static void link(
            String uuid,
            String username,
            String discordId
    ) {

        links.put(
                uuid,
                new LinkData(discordId, username)
        );

        save();
    }

    public static boolean isLinked(String uuid) {
        return links.containsKey(uuid);
    }

    public static boolean isDiscordLinked(
            String discordId
    ) {

        for (LinkData data : links.values()) {

            if (data.discordId().equals(discordId)) {
                return true;
            }
        }

        return false;
    }

    public static String getUsernameByDiscord(
            String discordId
    ) {

        for (LinkData data : links.values()) {

            if (data.discordId().equals(discordId)) {
                return data.username();
            }
        }

        return "Unknown";
    }

    public static String getDiscordIdByUsername(
            String username
    ) {
        for (LinkData data : links.values()) {
            if (data.username().equalsIgnoreCase(username)) {
                return data.discordId();
            }
        }

        return null;
    }

    public static void unlink(String uuid) {

        links.remove(uuid);

        save();
    }

    public static void unlinkDiscord(
            String discordId
    ) {

        String found = null;

        for (Map.Entry<String, LinkData> entry
                : links.entrySet()) {

            if (entry.getValue()
                    .discordId()
                    .equals(discordId)) {

                found = entry.getKey();

                break;
            }
        }

        if (found != null) {

            links.remove(found);

            save();
        }
    }

    public static void unlinkUsername(
            String username
    ) {

        String found = null;

        for (Map.Entry<String, LinkData> entry
                : links.entrySet()) {

            if (entry.getValue()
                    .username()
                    .equalsIgnoreCase(username)) {

                found = entry.getKey();

                break;
            }
        }

        if (found != null) {

            links.remove(found);

            save();
        }

    }
}
