package com.whobuscusyt.forgediscord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class UpdateChecker {

    public static String getLatestVersion() {
        try {
            URL url = new URL("https://api.modrinth.com/v2/project/forgediscord/version");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(url.openStream())
            );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

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