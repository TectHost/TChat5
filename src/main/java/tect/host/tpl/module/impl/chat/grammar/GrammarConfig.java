package tect.host.tpl.module.impl.chat.grammar;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;

import java.util.Set;
import java.util.stream.Collectors;

public final class GrammarConfig {

    private final boolean trimSpacesEnabled;

    private final boolean capEnabled;
    private final int capLetters;
    private final int capMinCharacters;

    private final boolean sentenceCaseEnabled;
    private final int sentenceCaseMinCharacters;

    private final boolean finalDotEnabled;
    private final String finalDotCharacter;
    private final int finalDotMinCharacters;
    private final Set<Character> finalDotIgnoredEndings;

    public GrammarConfig(@NonNull ConfigFile configFile) {
        var cfg = configFile.get();

        this.trimSpacesEnabled = cfg.getBoolean("trim-spaces.enabled", true);

        this.capEnabled = cfg.getBoolean("cap.enabled", true);
        this.capLetters = Math.max(0, cfg.getInt("cap.letters", 1));
        this.capMinCharacters = Math.max(0, cfg.getInt("cap.min-characters", 0));

        this.sentenceCaseEnabled = cfg.getBoolean("sentence-case.enabled", true);
        this.sentenceCaseMinCharacters = Math.max(0, cfg.getInt("sentence-case.min-characters", 0));

        this.finalDotEnabled = cfg.getBoolean("final-dot.enabled", true);
        this.finalDotCharacter = getDotCharacterSafe(cfg.getString("final-dot.character", "."));
        this.finalDotMinCharacters = Math.max(0, cfg.getInt("final-dot.min-characters", 0));
        this.finalDotIgnoredEndings = cfg.getStringList("final-dot.ignore-endings").stream()
                .filter(s -> s != null && !s.isEmpty())
                .map(s -> s.charAt(0))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static @NonNull String getDotCharacterSafe(String raw) {
        return (raw == null || raw.isEmpty()) ? "." : raw;
    }

    public boolean isTrimSpacesEnabled() { return trimSpacesEnabled; }

    public boolean isCapEnabled() { return capEnabled; }
    public int getCapLetters() { return capLetters; }
    public int getCapMinCharacters() { return capMinCharacters; }

    public boolean isSentenceCaseEnabled() { return sentenceCaseEnabled; }
    public int getSentenceCaseMinCharacters() { return sentenceCaseMinCharacters; }

    public boolean isFinalDotEnabled() { return finalDotEnabled; }
    public @NonNull String getFinalDotCharacter() { return finalDotCharacter; }
    public int getFinalDotMinCharacters() { return finalDotMinCharacters; }
    public @NonNull Set<Character> getFinalDotIgnoredEndings() { return finalDotIgnoredEndings; }
}