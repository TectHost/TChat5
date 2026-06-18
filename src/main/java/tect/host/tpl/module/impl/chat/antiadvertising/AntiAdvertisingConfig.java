package tect.host.tpl.module.impl.chat.antiadvertising;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.util.Utils;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class AntiAdvertisingConfig {

    private final Action action;
    private final char censorChar;

    private final boolean checkIpv4;
    private final boolean checkIpv6;
    private final boolean checkDomains;
    private final boolean checkUrls;

    private final List<String> whitelistedDomains;
    private final List<String> actions;

    private final @Nullable Pattern customIpv4Pattern;
    private final @Nullable Pattern customIpv6Pattern;
    private final @Nullable Pattern customDomainUrlPattern;

    public AntiAdvertisingConfig(@NonNull ConfigFile configFile, @NonNull Logger logger) {
        var cfg = configFile.get();

        this.action = Action.fromString(cfg.getString("action", "BLOCK"));
        this.censorChar = getCensorCharSafe(cfg.getString("censor-char"));

        this.checkIpv4 = cfg.getBoolean("filters.ipv4", true);
        this.checkIpv6 = cfg.getBoolean("filters.ipv6", true);
        this.checkDomains = cfg.getBoolean("filters.domains", true);
        this.checkUrls = cfg.getBoolean("filters.urls", true);

        this.whitelistedDomains = cfg.getStringList("whitelist.domains").stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.strip().toLowerCase())
                .toList();

        this.actions = cfg.getStringList("actions").stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::strip)
                .toList();

        this.customIpv4Pattern = compileOptional(cfg.getString("patterns.ipv4"), "patterns.ipv4", logger);
        this.customIpv6Pattern = compileOptional(cfg.getString("patterns.ipv6"), "patterns.ipv6", logger);
        this.customDomainUrlPattern = compileOptional(cfg.getString("patterns.domain-url"), "patterns.domain-url", logger);
    }

    private static char getCensorCharSafe(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return '*';
        return raw.charAt(0);
    }

    private static @Nullable Pattern compileOptional(@Nullable String raw, @NonNull String key, @NonNull Logger logger) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Pattern.compile(raw, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            Utils.log(logger, "WARNING", "[AntiAdvertising] Invalid regex at '" + key + "': " + e.getMessage() + ", using built-in default.");
            return null;
        }
    }

    public @NonNull Action getAction() { return action; }
    public char getCensorChar() { return censorChar; }
    public boolean isCheckIpv4() { return checkIpv4; }
    public boolean isCheckIpv6() { return checkIpv6; }
    public boolean isCheckDomains() { return checkDomains; }
    public boolean isCheckUrls() { return checkUrls; }
    public @NonNull List<String> getWhitelistedDomains() { return whitelistedDomains; }
    public @NonNull List<String> getActions() { return actions; }
    public @Nullable Pattern getCustomIpv4Pattern() { return customIpv4Pattern; }
    public @Nullable Pattern getCustomIpv6Pattern() { return customIpv6Pattern; }
    public @Nullable Pattern getCustomDomainUrlPattern() { return customDomainUrlPattern; }
}