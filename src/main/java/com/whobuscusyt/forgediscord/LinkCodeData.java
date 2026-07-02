package com.whobuscusyt.forgediscord;

public record LinkCodeData(
        String uuid,
        String username,
        long expiresAt
) {

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}
