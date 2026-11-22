package com.yourname.totemrunes.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class SignatureUtil {

    private static final String ALGORITHM = "HmacSHA256";

    private SignatureUtil() {
    }

    public static String sign(String secret, String runeId) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] raw = mac.doFinal(runeId.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign rune id", e);
        }
    }

    public static boolean verify(String secret, String runeId, String signature) {
        if (secret == null || runeId == null || signature == null || signature.isBlank()) {
            return false;
        }
        String expected = sign(secret, runeId);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }
}
