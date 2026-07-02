package com.whobuscusyt.forgediscord;

import java.util.Map;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class LinkCodeManager {

    private static final Map<String, LinkCodeData> codes =
            new ConcurrentHashMap<>();

    private static final SecureRandom random =
            new SecureRandom();

    private static final long CODE_LIFETIME_MS =
            TimeUnit.MINUTES.toMillis(10);

    public static String createCode(
            String uuid,
            String username
    ) {

        codes.entrySet().removeIf(entry -> entry.getValue().isExpired());
        codes.entrySet().removeIf(entry -> entry.getValue().uuid().equals(uuid));

        String code;
        do {

            code = String.valueOf(
                    10000 + random.nextInt(90000)
            );

        } while (codes.containsKey(code));

        codes.put(
                code,
                new LinkCodeData(
                        uuid,
                        username,
                        System.currentTimeMillis() + CODE_LIFETIME_MS
                )
        );

        return code;
    }

    public static LinkCodeData get(String code) {
        LinkCodeData data = codes.get(code);
        if (data != null && data.isExpired()) {
            codes.remove(code);
            return null;
        }
        return data;
    }

    public static void remove(String code) {
        codes.remove(code);
    }
}
