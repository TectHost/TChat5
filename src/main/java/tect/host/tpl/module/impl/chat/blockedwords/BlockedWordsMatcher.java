package tect.host.tpl.module.impl.chat.blockedwords;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BlockedWordsMatcher {

    private static final ThreadLocal<int[]> ROW_BUFFER = ThreadLocal.withInitial(() -> new int[32]);

    private BlockedWordsMatcher() {}

    /**
     * Returns the first blocked word found in rawMessage, or null if clean
     */
    public static @NonNull MatchResult check(@NonNull String rawMessage, @NonNull Map<String, String> wordCache) {
        if (wordCache.isEmpty()) return MatchResult.CLEAN;

        List<String> tokens = tokenize(rawMessage);
        if (tokens.isEmpty()) return MatchResult.CLEAN;

        for (Map.Entry<String, String> entry : wordCache.entrySet()) {
            String normWord = entry.getValue();
            if (normWord.isEmpty()) continue;
            int threshold = threshold(normWord.length());
            for (String token : tokens) {
                if (token.isEmpty()) continue;
                if (containsWithinDistance(token, normWord, threshold)) {
                    return new MatchResult(true, entry.getKey());
                }
            }
        }
        return MatchResult.CLEAN;
    }

    /**
     * Replaces each matched region in rawMessage with censorChar
     */
    public static @Nullable String censor(@NonNull String rawMessage, @NonNull Map<String, String> wordCache, char censorChar) {
        if (wordCache.isEmpty()) return null;

        List<Token> tokens = tokenizeWithIndices(rawMessage);
        if (tokens.isEmpty()) return null;

        boolean[] censored = new boolean[rawMessage.length()];
        boolean anyCensored = false;

        for (Map.Entry<String, String> entry : wordCache.entrySet()) {
            String normWord = entry.getValue();
            if (normWord.isEmpty()) continue;
            int threshold = threshold(normWord.length());
            int wLen = normWord.length();
            int minWin = Math.max(1, wLen - threshold);
            int maxWin = wLen + threshold;

            for (Token token : tokens) {
                String stripped = token.stripped();
                if (stripped.isEmpty()) continue;

                int start = 0;
                while (start <= stripped.length() - minWin) {
                    boolean matched = false;
                    for (int winLen = minWin; winLen <= maxWin && start + winLen <= stripped.length(); winLen++) {
                        if (levenshtein(stripped, start, winLen, normWord) <= threshold) {
                            for (int k = start; k < start + winLen; k++) {
                                censored[token.origIndices()[k]] = true;
                                anyCensored = true;
                            }
                            start += winLen;
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) start++;
                }
            }
        }

        if (!anyCensored) return null;

        char[] result = rawMessage.toCharArray();
        for (int i = 0; i < result.length; i++) {
            if (censored[i]) result[i] = censorChar;
        }
        return new String(result);
    }

    /**
     * censors every alphanumeric character of each
     * space delimited token that contains at least one matched character
     */
    public static @Nullable String censorAll(@NonNull String rawMessage, @NonNull Map<String, String> wordCache, char censorChar) {
        if (wordCache.isEmpty()) return null;

        List<Token> tokens = tokenizeWithIndices(rawMessage);
        if (tokens.isEmpty()) return null;

        boolean[] censoredOrig = new boolean[rawMessage.length()];
        boolean anyCensored = false;

        for (Map.Entry<String, String> entry : wordCache.entrySet()) {
            String normWord = entry.getValue();
            if (normWord.isEmpty()) continue;
            int threshold = threshold(normWord.length());

            for (Token token : tokens) {
                String stripped = token.stripped();
                if (stripped.isEmpty()) continue;

                if (containsWithinDistance(stripped, normWord, threshold)) {
                    for (int origIdx : token.origIndices()) {
                        censoredOrig[origIdx] = true;
                        anyCensored = true;
                    }
                }
            }
        }

        if (!anyCensored) return null;

        char[] result = rawMessage.toCharArray();
        for (int i = 0; i < result.length; i++) {
            if (censoredOrig[i]) result[i] = censorChar;
        }
        return new String(result);
    }

    private static @NonNull List<String> tokenize(@NonNull String s) {
        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                if (!cur.isEmpty()) { tokens.add(cur.toString()); cur.setLength(0); }
            } else if (isAlphaNum(c)) {
                cur.append(Character.toLowerCase(c));
            }
        }
        if (!cur.isEmpty()) tokens.add(cur.toString());
        return tokens;
    }

    private static @NonNull List<Token> tokenizeWithIndices(@NonNull String s) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder strippedBuf = new StringBuilder();
        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                if (!strippedBuf.isEmpty()) {
                    tokens.add(new Token(strippedBuf.toString(), toIntArray(indices)));
                    strippedBuf.setLength(0);
                    indices.clear();
                }
            } else if (isAlphaNum(c)) {
                strippedBuf.append(Character.toLowerCase(c));
                indices.add(i);
            }
        }
        if (!strippedBuf.isEmpty()) {
            tokens.add(new Token(strippedBuf.toString(), toIntArray(indices)));
        }
        return tokens;
    }

    private static int @NonNull [] toIntArray(@NonNull List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    /**
     * Sliding window fuzzy search over stripped text
     */
    private static boolean containsWithinDistance(@NonNull String text, @NonNull String word, int threshold) {
        int wLen = word.length(), tLen = text.length();

        int minWin = Math.max(1, wLen - threshold);
        int maxWin = wLen + threshold;

        for (int start = 0; start <= tLen - minWin; start++) {
            for (int winLen = minWin; winLen <= maxWin && start + winLen <= tLen; winLen++) {
                if (levenshtein(text, start, winLen, word) <= threshold) return true;
            }
        }

        return false;
    }

    private static int levenshtein(String a, int aStart, int aLen, @NonNull String b) {
        int lb = b.length();
        if (aLen == 0) return lb;
        if (lb == 0) return aLen;

        int[] row = ROW_BUFFER.get();
        if (row.length <= lb) { row = new int[lb + 1]; ROW_BUFFER.set(row); }

        for (int j = 0; j <= lb; j++) row[j] = j;
        for (int i = 1; i <= aLen; i++) {
            int diag = i - 1;
            row[0] = i;
            char ca = a.charAt(aStart + i - 1);
            for (int j = 1; j <= lb; j++) {
                int temp = row[j];
                row[j] = ca == b.charAt(j - 1) ? diag : 1 + Math.min(diag, Math.min(row[j], row[j - 1]));
                diag = temp;
            }
        }
        return row[lb];
    }

    public static @NonNull String stripWord(@NonNull String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isAlphaNum(c)) sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private static boolean isAlphaNum(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }

    private static int threshold(int wordLen) {
        if (wordLen <= 4) return 0;
        if (wordLen <= 7) return 1;
        return 2;
    }

    private record Token(@NonNull String stripped, int[] origIndices) {}

    public record MatchResult(boolean matched, String matchedWord) {
        static final MatchResult CLEAN = new MatchResult(false, null);
    }
}