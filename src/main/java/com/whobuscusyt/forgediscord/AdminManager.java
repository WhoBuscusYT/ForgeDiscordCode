package com.whobuscusyt.forgediscord;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AdminManager {

    private static final Set<UUID> ADMINS = new HashSet<>();
    private static final File FILE = new File("config/forgediscord_admins.json");
    private static final Gson GSON = new Gson();

    public static void load() {
        try {
            if (!FILE.exists()) return;

            FileReader reader = new FileReader(FILE);
            Type type = new TypeToken<Set<String>>(){}.getType();

            Set<String> data = GSON.fromJson(reader, type);
            reader.close();

            ADMINS.clear();

            for (String s : data) {
                ADMINS.add(UUID.fromString(s));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            FILE.getParentFile().mkdirs();

            FileWriter writer = new FileWriter(FILE);

            Set<String> data = new HashSet<>();
            for (UUID uuid : ADMINS) {
                data.add(uuid.toString());
            }

            GSON.toJson(data, writer);
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void add(UUID uuid) {
        ADMINS.add(uuid);
        save();
    }

    public static void remove(UUID uuid) {
        ADMINS.remove(uuid);
        save();
    }

    public static boolean isAdmin(UUID uuid) {
        return ADMINS.contains(uuid);
    }

    public static Set<UUID> getAdmins() {
        return ADMINS;
    }
}