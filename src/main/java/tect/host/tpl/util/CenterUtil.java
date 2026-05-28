package tect.host.tpl.util;

import org.jspecify.annotations.NonNull;

public final class CenterUtil {

    private CenterUtil() {}

    private static final int CHAT_WIDTH_PX = 320;
    private static final int SPACE_WIDTH = 4 + 1; // char width + gap

    public static @NonNull String center(@NonNull String line) {
        if (!line.startsWith("%center%")) return line;

        String content = line.substring("%center%".length());
        String plain = stripFormatting(content);
        int textPx = measurePixels(plain);

        int halfPadding = Math.max(0, (CHAT_WIDTH_PX - textPx) / 2);
        int spaces = halfPadding / SPACE_WIDTH;

        if (spaces == 0) return content;

        return " ".repeat(spaces) + content;
    }

    static int measurePixels(@NonNull String plain) {
        int total = 0;
        for (int i = 0; i < plain.length(); i++) {
            total += charWidth(plain.charAt(i)) + 1; // +1 for inter-glyph gap
        }
        return total;
    }

    /** Removes all formatting tokens */
    static @NonNull String stripFormatting(@NonNull String text) {
        // MiniMessage tags
        text = text.replaceAll("<[^>]*>", "");
        // Legacy &X codes
        text = text.replaceAll("&[0-9a-fA-F k-orK-OR]", "");
        return text;
    }

    static int charWidth(char c) {
        return switch (c) {
            case '!', '\'', '.', ',', ':', ';', 'i', '|' -> 2;
            case 'l', '`', '·' -> 3;
            case ' ', '"', '(', ')', '*', '[', ']', 't', 'I', '{', '}' -> 4;
            case '<', '>', '?', 'f', 'k', 'r' -> 5;
            case '1' -> 6;
            case '~' -> 7;
            // most ASCII letters/digits are 6 px wide
            default -> 6;
        };
    }
}