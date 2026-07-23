package tect.host.tpl.module.impl.chat.antiadvertising;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.util.text.CensorUtil;
import tect.host.tpl.util.text.NormalizedText;
import tect.host.tpl.util.text.ObfuscationNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AntiAdvertisingMatcher {

    private AntiAdvertisingMatcher() {}

    private static final Pattern IPV4 = Pattern.compile(
            "(?<![\\w.])(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}" +
                    "(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)(?::\\d{1,5})?|(?=\\s|$))" +
                    "(?![\\w.])",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern IPV6 = Pattern.compile(
            "(?:" +
                    "\\[?[0-9a-f]{1,4}(?::[0-9a-f]{0,4}){2,7}]?" +
                    "|::(?:[0-9a-f]{1,4}:){0,6}[0-9a-f]{1,4}" +
                    "|[0-9a-f]{1,4}::(?:[0-9a-f]{1,4}:){0,5}[0-9a-f]{1,4}" +
                    ")(?::\\d{1,5})?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DOMAIN_URL = Pattern.compile(
            "(?:https?://|ftp://|www\\.)?" +
                    "(?:[a-z0-9](?:[a-z0-9\\-]{0,61}[a-z0-9])?\\.)" +
                    "+" +
                    "(?:com|net|org|gg|io|me|co|uk|de|fr|es|ru|xyz|tk|top|club|site|online|store|" +
                    "info|biz|tv|us|ca|au|jp|cn|br|in|pl|nl|se|no|fi|dk|be|ch|at|nz|mx|ar|cl|" +
                    "cc|eu|mobi|pro|name|coop|aero|museum|gov|mil|edu|int)" +
                    "(?:/\\S*)?",
            Pattern.CASE_INSENSITIVE);

    private record Span(int start, int end, @NonNull String type) {}

    public static @NonNull AntiAdvertisingResult check(@NonNull String raw, @NonNull AntiAdvertisingConfig config) {
        NormalizedText normalized = ObfuscationNormalizer.normalizeObfuscation(raw);
        Span first = findFirst(normalized.text(), config);
        return first != null ? new AntiAdvertisingResult(true, first.type()) : AntiAdvertisingResult.CLEAN;
    }

    public static @Nullable String censor(@NonNull String raw, @NonNull AntiAdvertisingConfig config, char censorChar) {
        NormalizedText normalized = ObfuscationNormalizer.normalizeObfuscation(raw);
        List<Span> spans = findAll(normalized.text(), config);
        if (spans.isEmpty()) return null;

        boolean[] mask = new boolean[raw.length()];
        for (Span span : spans) normalized.markRaw(mask, span.start(), span.end());

        String result = CensorUtil.applyMask(raw, mask, censorChar);
        return result != null ? result : CensorUtil.censorFull(raw, censorChar);
    }

    private static @Nullable Span findFirst(@NonNull String normalized, @NonNull AntiAdvertisingConfig config) {
        if (config.isCheckIpv4()) {
            Pattern p = config.getCustomIpv4Pattern() != null ? config.getCustomIpv4Pattern() : IPV4;
            Matcher m = p.matcher(normalized);
            if (m.find()) return new Span(m.start(), m.end(), "ipv4");
        }

        if (config.isCheckIpv6()) {
            Pattern p = config.getCustomIpv6Pattern() != null ? config.getCustomIpv6Pattern() : IPV6;
            Matcher m = p.matcher(normalized);
            if (m.find()) return new Span(m.start(), m.end(), "ipv6");
        }

        if (config.isCheckDomains() || config.isCheckUrls()) {
            Pattern p = config.getCustomDomainUrlPattern() != null ? config.getCustomDomainUrlPattern() : DOMAIN_URL;
            Matcher m = p.matcher(normalized);
            while (m.find()) {
                Span span = classify(m, config);
                if (span != null) return span;
            }
        }

        return null;
    }

    private static @NonNull List<Span> findAll(@NonNull String normalized, @NonNull AntiAdvertisingConfig config) {
        List<Span> spans = new ArrayList<>();

        if (config.isCheckIpv4()) {
            Pattern p = config.getCustomIpv4Pattern() != null ? config.getCustomIpv4Pattern() : IPV4;
            Matcher m = p.matcher(normalized);
            while (m.find()) spans.add(new Span(m.start(), m.end(), "ipv4"));
        }

        if (config.isCheckIpv6()) {
            Pattern p = config.getCustomIpv6Pattern() != null ? config.getCustomIpv6Pattern() : IPV6;
            Matcher m = p.matcher(normalized);
            while (m.find()) spans.add(new Span(m.start(), m.end(), "ipv6"));
        }

        if (config.isCheckDomains() || config.isCheckUrls()) {
            Pattern p = config.getCustomDomainUrlPattern() != null ? config.getCustomDomainUrlPattern() : DOMAIN_URL;
            Matcher m = p.matcher(normalized);
            while (m.find()) {
                Span span = classify(m, config);
                if (span != null) spans.add(span);
            }
        }

        return spans;
    }

    private static @Nullable Span classify(@NonNull Matcher m, @NonNull AntiAdvertisingConfig config) {
        String match = m.group().toLowerCase();
        if (isWhitelisted(match, config.getWhitelistedDomains())) return null;

        String type = resolveType(match);
        if (type.equals("url") && !config.isCheckUrls()) return null;
        if (type.equals("domain") && !config.isCheckDomains()) return null;

        return new Span(m.start(), m.end(), type);
    }

    private static @NonNull String resolveType(@NonNull String match) {
        return (match.startsWith("http") || match.startsWith("ftp") || match.startsWith("www")) ? "url" : "domain";
    }

    private static boolean isWhitelisted(@NonNull String match, @NonNull List<String> whitelist) {
        for (String w : whitelist) {
            if (match.contains(w)) return true;
        }
        return false;
    }
}