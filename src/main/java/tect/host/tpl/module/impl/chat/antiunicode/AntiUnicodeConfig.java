package tect.host.tpl.module.impl.chat.antiunicode;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.util.Utils;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public final class AntiUnicodeConfig {

    private final AntiUnicodeAction action;
    private final char censorChar;

    private final boolean checkBidiControl;
    private final boolean checkZeroWidth;
    private final boolean checkControlChars;
    private final boolean checkZalgo;
    private final boolean checkFullwidth;
    private final boolean checkPrivateUseArea;
    private final boolean checkEmoji;
    private final boolean checkScripts;

    private final int maxCombiningMarks;
    private final Set<Character.UnicodeScript> allowedScripts;

    private final List<String> actions;

    public AntiUnicodeConfig(@NonNull ConfigFile configFile, @NonNull Logger logger) {
        var cfg = configFile.get();

        this.action = AntiUnicodeAction.fromString(cfg.getString("action", "BLOCK"));
        this.censorChar = getCensorCharSafe(cfg.getString("censor-char"));

        this.checkBidiControl = cfg.getBoolean("filters.bidi-control", true);
        this.checkZeroWidth = cfg.getBoolean("filters.zero-width", true);
        this.checkControlChars = cfg.getBoolean("filters.control-chars", true);
        this.checkZalgo = cfg.getBoolean("filters.zalgo", true);
        this.checkFullwidth = cfg.getBoolean("filters.fullwidth", true);
        this.checkPrivateUseArea = cfg.getBoolean("filters.private-use-area", true);
        this.checkEmoji = cfg.getBoolean("filters.emoji", false);
        this.checkScripts = cfg.getBoolean("filters.scripts", false);

        this.maxCombiningMarks = Math.max(0, cfg.getInt("zalgo.max-combining-marks", 2));

        this.allowedScripts = parseScripts(cfg.getStringList("scripts.allowed"), logger);

        this.actions = cfg.getStringList("actions").stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::strip)
                .toList();
    }

    private static char getCensorCharSafe(@org.jspecify.annotations.Nullable String raw) {
        if (raw == null || raw.isEmpty()) return '*';
        return raw.charAt(0);
    }

    private static @NonNull Set<Character.UnicodeScript> parseScripts(@NonNull List<String> raw, @NonNull Logger logger) {
        Set<Character.UnicodeScript> scripts = EnumSet.noneOf(Character.UnicodeScript.class);

        for (String s : raw) {
            if (s == null || s.isBlank()) continue;
            try {
                scripts.add(Character.UnicodeScript.forName(s.strip()));
            } catch (IllegalArgumentException e) {
                Utils.log(logger, "WARNING", "[AntiUnicode] Unknown script '" + s + "' in scripts.allowed, ignoring.");
            }
        }

        scripts.add(Character.UnicodeScript.COMMON);
        scripts.add(Character.UnicodeScript.INHERITED);

        if (scripts.size() == 2) {
            scripts.add(Character.UnicodeScript.LATIN);
        }

        return scripts;
    }

    public @NonNull AntiUnicodeAction getAction() { return action; }
    public char getCensorChar() { return censorChar; }
    public boolean isCheckBidiControl() { return checkBidiControl; }
    public boolean isCheckZeroWidth() { return checkZeroWidth; }
    public boolean isCheckControlChars() { return checkControlChars; }
    public boolean isCheckZalgo() { return checkZalgo; }
    public boolean isCheckFullwidth() { return checkFullwidth; }
    public boolean isCheckPrivateUseArea() { return checkPrivateUseArea; }
    public boolean isCheckEmoji() { return checkEmoji; }
    public boolean isCheckScripts() { return checkScripts; }
    public int getMaxCombiningMarks() { return maxCombiningMarks; }
    public @NonNull Set<Character.UnicodeScript> getAllowedScripts() { return allowedScripts; }
    public @NonNull List<String> getActions() { return actions; }
}