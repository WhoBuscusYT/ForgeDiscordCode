package com.whobuscusyt.forgediscord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UpdateChecker {

    public static String getLatestVersion() {
        try {
            URL url = new URL("https://api.modrinth.com/v2/project/forgediscord/version");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "ForgeDiscord/" + ForgeDiscord.VERSION);

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            String json = response.toString();

            int index = json.indexOf("\"version_number\":\"");

            if (index == -1) return null;

            int start = index + 18;
            int end = json.indexOf("\"", start);

            return json.substring(start, end);

        } catch (Exception e) {
            return null;
        }
    }
}
