package com.yourname.totemrunes.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class TextUtil {

    private TextUtil() {
    }

    public static Component noItalic(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    public static Component text(String text, String hex) {
        return noItalic(Component.text(text).color(TextColor.fromHexString(hex)));
    }

    public static Component text(String text, TextColor color) {
        return noItalic(Component.text(text).color(color));
    }

    public static String plain(Component component) {
        if (component == null) {
            return "";
        }
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static String formatTime(long millis) {
        long totalSeconds = Math.max(0L, (millis + 999L) / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes > 0) {
            return minutes + "м " + String.format("%02d", seconds) + "с";
        }
        return seconds + "с";
    }
}
