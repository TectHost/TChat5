package tect.host.tpl.module.impl.chat.antispam;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;

import java.util.List;
import java.util.regex.Pattern;

public final class AntiSpamConfig {

    private final AntiSpamAction antiSpamAction;
    private final char censorChar;

    private final boolean checkRepeatedChars;
    private final Pattern repeatedCharsPattern;

    private final boolean checkRepeatedPattern;
    private final Pattern repeatedPatternPattern;

    private final boolean checkRepeatedWords;
    private final Pattern repeatedWordsPattern;

    private final boolean checkCharDiversity;
    private final int charDiversityMinLength;
    private final double charDiversityMinRatio;

    private final List<String> whitelistedWords;
    private final List<String> actions;

    public AntiSpamConfig(@NonNull ConfigFile configFile) {
        var cfg = configFile.get();

        this.antiSpamAction = AntiSpamAction.fromString(cfg.getString("action", "BLOCK"));
        this.censorChar = getCensorCharSafe(cfg.getString("censor-char"));

        this.checkRepeatedChars = cfg.getBoolean("checks.repeated-chars.enabled", true);
        int repeatedCharsThreshold = Math.max(3, cfg.getInt("checks.repeated-chars.threshold", 5));
        this.repeatedCharsPattern = Pattern.compile("(.)\\1{" + (repeatedCharsThreshold - 1) + ",}");

        this.checkRepeatedPattern = cfg.getBoolean("checks.repeated-pattern.enabled", true);
        int repeatedPatternMinLength = Math.max(2, cfg.getInt("checks.repeated-pattern.min-length", 2));
        int repeatedPatternMaxLength = Math.max(repeatedPatternMinLength, cfg.getInt("checks.repeated-pattern.max-length", 4));
        int repeatedPatternMinRepeats = Math.max(2, cfg.getInt("checks.repeated-pattern.min-repeats", 3));
        this.repeatedPatternPattern = Pattern.compile("(.{" + repeatedPatternMinLength + "," + repeatedPatternMaxLength + "})\\1{" + (repeatedPatternMinRepeats - 1) + ",}");

        this.checkRepeatedWords = cfg.getBoolean("checks.repeated-words.enabled", true);
        int repeatedWordsThreshold = Math.max(2, cfg.getInt("checks.repeated-words.threshold", 3));
        this.repeatedWordsPattern = Pattern.compile(
                "\\b(\\p{L}+)\\b(?:\\s+\\1\\b){" + (repeatedWordsThreshold - 1) + ",}",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );

        this.checkCharDiversity = cfg.getBoolean("checks.char-diversity.enabled", false);
        this.charDiversityMinLength = Math.max(1, cfg.getInt("checks.char-diversity.min-length", 12));
        this.charDiversityMinRatio = clampRatio(cfg.getDouble("checks.char-diversity.min-ratio", 0.3));

        this.whitelistedWords = cfg.getStringList("whitelist.words").stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.strip().toLowerCase())
                .toList();

        this.actions = cfg.getStringList("actions").stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::strip)
                .toList();
    }

    private static char getCensorCharSafe(String raw) {
        if (raw == null || raw.isEmpty()) return '*';
        return raw.charAt(0);
    }

    private static double clampRatio(double ratio) {
        if (ratio < 0.0) return 0.0;
        return Math.min(ratio, 1.0);
    }

    public @NonNull AntiSpamAction getAction() { return antiSpamAction; }
    public char getCensorChar() { return censorChar; }

    public boolean isCheckRepeatedChars() { return checkRepeatedChars; }
    public @NonNull Pattern getRepeatedCharsPattern() { return repeatedCharsPattern; }

    public boolean isCheckRepeatedPattern() { return checkRepeatedPattern; }
    public @NonNull Pattern getRepeatedPatternPattern() { return repeatedPatternPattern; }

    public boolean isCheckRepeatedWords() { return checkRepeatedWords; }
    public @NonNull Pattern getRepeatedWordsPattern() { return repeatedWordsPattern; }

    public boolean isCheckCharDiversity() { return checkCharDiversity; }
    public int getCharDiversityMinLength() { return charDiversityMinLength; }
    public double getCharDiversityMinRatio() { return charDiversityMinRatio; }

    public @NonNull List<String> getWhitelistedWords() { return whitelistedWords; }
    public @NonNull List<String> getActions() { return actions; }
}