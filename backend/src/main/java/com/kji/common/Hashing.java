package com.kji.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.stream.Collectors;

public final class Hashing {

    private static final char UNIT_SEPARATOR = 0x1f;

    private Hashing() {
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value == null
                    ? new byte[0]
                    : value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static String sha256OfFields(String... fields) {
        String joined = Arrays.stream(fields)
                .map(field -> field == null ? "" : field)
                .collect(Collectors.joining(String.valueOf(UNIT_SEPARATOR)));
        return sha256(joined);
    }
}
