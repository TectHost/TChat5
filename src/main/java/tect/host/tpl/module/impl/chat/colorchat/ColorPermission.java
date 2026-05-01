package tect.host.tpl.module.impl.chat.colorchat;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Every color/decoration a player may use in chat, paired with:
 *  - the permission node that grants it
 *  - the canonical MiniMessage tag (primary alias)
 *  - all MiniMessage tag aliases it responds to
 *  - the legacy '&' codes it maps to (empty for MiniMessage-only features)
 *.
 * Naming convention: tchat.colorchat.<category>.<n>
 *.
 * BLOCKED_TAGS are action/event tags NEVER allowed regardless of permission
 */
public enum ColorPermission {

    // Colors ---------------------------------------------------------------------------------------------
    BLACK        ("tchat.colorchat.color.black",        "black",        Set.of("black"),                Set.of("0")),
    DARK_BLUE    ("tchat.colorchat.color.dark_blue",    "dark_blue",    Set.of("dark_blue"),            Set.of("1")),
    DARK_GREEN   ("tchat.colorchat.color.dark_green",   "dark_green",   Set.of("dark_green"),           Set.of("2")),
    DARK_AQUA    ("tchat.colorchat.color.dark_aqua",    "dark_aqua",    Set.of("dark_aqua"),            Set.of("3")),
    DARK_RED     ("tchat.colorchat.color.dark_red",     "dark_red",     Set.of("dark_red"),             Set.of("4")),
    DARK_PURPLE  ("tchat.colorchat.color.dark_purple",  "dark_purple",  Set.of("dark_purple"),          Set.of("5")),
    GOLD         ("tchat.colorchat.color.gold",         "gold",         Set.of("gold"),                 Set.of("6")),
    GRAY         ("tchat.colorchat.color.gray",         "gray",         Set.of("gray"),                 Set.of("7")),
    DARK_GRAY    ("tchat.colorchat.color.dark_gray",    "dark_gray",    Set.of("dark_gray"),            Set.of("8")),
    BLUE         ("tchat.colorchat.color.blue",         "blue",         Set.of("blue"),                 Set.of("9")),
    GREEN        ("tchat.colorchat.color.green",        "green",        Set.of("green"),                Set.of("a")),
    AQUA         ("tchat.colorchat.color.aqua",         "aqua",         Set.of("aqua"),                 Set.of("b")),
    RED          ("tchat.colorchat.color.red",          "red",          Set.of("red"),                  Set.of("c")),
    LIGHT_PURPLE ("tchat.colorchat.color.light_purple", "light_purple", Set.of("light_purple"),         Set.of("d")),
    YELLOW       ("tchat.colorchat.color.yellow",       "yellow",       Set.of("yellow"),               Set.of("e")),
    WHITE        ("tchat.colorchat.color.white",        "white",        Set.of("white"),                Set.of("f")),

    // Text decorations -----------------------------------------------------------------------------------
    BOLD          ("tchat.colorchat.format.bold",          "bold",          Set.of("bold", "b"),          Set.of("l")),
    ITALIC        ("tchat.colorchat.format.italic",        "italic",        Set.of("italic", "em", "i"),  Set.of("o")),
    UNDERLINED    ("tchat.colorchat.format.underlined",    "underlined",    Set.of("underlined", "u"),     Set.of("n")),
    STRIKETHROUGH ("tchat.colorchat.format.strikethrough", "strikethrough", Set.of("strikethrough", "st"), Set.of("m")),
    OBFUSCATED    ("tchat.colorchat.format.obfuscated",    "obfuscated",    Set.of("obfuscated", "obf"),   Set.of("k")),
    RESET         ("tchat.colorchat.format.reset",         "reset",         Set.of("reset"),               Set.of("r")),

    // Advanced MM actions -------------------------------------------------------------------------------------
    HEX_COLOR ("tchat.colorchat.advanced.hex",      "color",    Set.of("color", "colour", "c"), Set.of()),
    GRADIENT  ("tchat.colorchat.advanced.gradient", "gradient", Set.of("gradient", "gr"),       Set.of()),
    RAINBOW   ("tchat.colorchat.advanced.rainbow",  "rainbow",  Set.of("rainbow", "rb"),        Set.of());

    // Blocked action tags from MM
    public static final Set<String> BLOCKED_TAGS = Set.of(
            "click", "hover", "insertion",
            "keybind", "lang", "translatable",
            "selector", "score", "nbt",
            "newline", "br"
    );

    private final String node;
    /**
     * Pre-built {@code "<primaryTag>"} string
     * Stored as a field to avoid string concatenation on every legacy code hit
     */
    private final String primaryTagWrapped;
    /** All MiniMessage tag aliases (lowercase) */
    private final Set<String> miniTags;
    /** Legacy code chars (lowercase, without '&') */
    private final Set<String> legacyCodes;

    ColorPermission(@NonNull String node, @NonNull String primaryTag, @NonNull Set<String> miniTags, @NonNull Set<String> legacyCodes) {
        this.node              = node;
        this.primaryTagWrapped = "<" + primaryTag + ">";
        this.miniTags          = miniTags;
        this.legacyCodes       = legacyCodes;
    }

    public @NonNull String getNode()              { return node; }
    public @NonNull String getPrimaryTagWrapped() { return primaryTagWrapped; }

    private static final Map<String, ColorPermission> BY_MINI_TAG;
    private static final Map<String, ColorPermission> BY_LEGACY_CODE;

    static {
        BY_MINI_TAG = Arrays.stream(values())
                .flatMap(cp -> cp.miniTags.stream().map(tag -> Map.entry(tag, cp)))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        BY_LEGACY_CODE = Arrays.stream(values())
                .flatMap(cp -> cp.legacyCodes.stream().map(code -> Map.entry(code, cp)))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static @NonNull Optional<ColorPermission> byMiniTag(@NonNull String tag) {
        return Optional.ofNullable(BY_MINI_TAG.get(tag.toLowerCase()));
    }

    public static @NonNull Optional<ColorPermission> byLegacyCode(@NonNull String code) {
        return Optional.ofNullable(BY_LEGACY_CODE.get(code.toLowerCase()));
    }
}