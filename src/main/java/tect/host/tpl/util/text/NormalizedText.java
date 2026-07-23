package tect.host.tpl.util.text;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;

/**
 * Normalized text with the exact raw range for each emitted character, allowing
 * matchers to work on cleaned text while mapping matches back to the original
 */
public final class NormalizedText {

    private final String text;
    private final int[] rawStart;
    private final int[] rawEnd;

    private NormalizedText(@NonNull String text, int @NonNull [] rawStart, int @NonNull [] rawEnd) {
        this.text = text;
        this.rawStart = rawStart;
        this.rawEnd = rawEnd;
    }

    public @NonNull String text() {
        return text;
    }

    /** Marks the raw range corresponding to [normStart, normEnd) in the normalized text */
    public void markRaw(boolean @NonNull [] rawMask, int normStart, int normEnd) {
        if (normStart < 0 || normEnd > text.length() || normStart >= normEnd) return;

        int from = rawStart[normStart];
        int to = rawEnd[normEnd - 1];
        for (int i = from; i < to && i < rawMask.length; i++) rawMask[i] = true;
    }

    public static @NonNull Builder builder(@NonNull String raw) {
        return new Builder(raw.length());
    }

    public static final class Builder {

        private final StringBuilder text = new StringBuilder();
        private int[] rawStart;
        private int[] rawEnd;
        private int size = 0;

        private Builder(int rawLength) {
            int cap = Math.max(16, rawLength);
            rawStart = new int[cap];
            rawEnd = new int[cap];
        }

        public void emit(char c, int from, int to) {
            if (size == rawStart.length) {
                int newCap = rawStart.length * 2;
                rawStart = Arrays.copyOf(rawStart, newCap);
                rawEnd = Arrays.copyOf(rawEnd, newCap);
            }
            text.append(c);
            rawStart[size] = from;
            rawEnd[size] = to;
            size++;
        }

        public @NonNull NormalizedText build() {
            return new NormalizedText(text.toString(), Arrays.copyOf(rawStart, size), Arrays.copyOf(rawEnd, size));
        }
    }
}