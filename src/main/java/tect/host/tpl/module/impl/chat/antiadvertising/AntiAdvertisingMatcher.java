package tect.host.tpl.module.impl.chat.antiadvertising;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.util.CensorUtil;
import tect.host.tpl.util.MaskUtil;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AntiAdvertisingMatcher {

    private AntiAdvertisingMatcher() {}

    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B-\\u200D\\uFEFF\\u00AD\\u2060\\u180E]");
    private static final Pattern DOT_WORD = Pattern.compile("\\(dot\\)|\\[dot]|\\{dot}|(?<![a-z0-9])dot(?![a-z0-9])", Pattern.CASE_INSENSITIVE);
    private static final Pattern COLON_WORD = Pattern.compile("\\(colon\\)|\\[colon]", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPACE_AROUND_PUNCT = Pattern.compile("\\s*([.:])\\s*");

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

    public static @NonNull AntiAdvertisingResult check(@NonNull String raw, @NonNull AntiAdvertisingConfig config) {
        String n = normalise(raw);

        if (config.isCheckIpv4()) {
            Pattern p = config.getCustomIpv4Pattern() != null ? config.getCustomIpv4Pattern() : IPV4;
            if (p.matcher(n).find()) return new AntiAdvertisingResult(true, "ipv4");
        }

        if (config.isCheckIpv6()) {
            Pattern p = config.getCustomIpv6Pattern() != null ? config.getCustomIpv6Pattern() : IPV6;
            if (p.matcher(n).find()) return new AntiAdvertisingResult(true, "ipv6");
        }

        if (config.isCheckDomains() || config.isCheckUrls()) {
            Pattern p = config.getCustomDomainUrlPattern() != null ? config.getCustomDomainUrlPattern() : DOMAIN_URL;
            Matcher m = p.matcher(n);
            while (m.find()) {
                String match = m.group().toLowerCase();
                if (isWhitelisted(match, config.getWhitelistedDomains())) continue;
                String type = resolveType(match);
                if (type.equals("url") && !config.isCheckUrls()) continue;
                if (type.equals("domain") && !config.isCheckDomains()) continue;
                return new AntiAdvertisingResult(true, type);
            }
        }

        return AntiAdvertisingResult.CLEAN;
    }

    public static @Nullable String censor(@NonNull String raw, @NonNull AntiAdvertisingConfig config, char censorChar) {
        if (!check(raw, config).matched()) return null;

        boolean[] mask = new boolean[raw.length()];

        if (config.isCheckIpv4()) {
            Pattern p = config.getCustomIpv4Pattern() != null ? config.getCustomIpv4Pattern() : IPV4;
            MaskUtil.applyMatcher(p.matcher(raw), mask);
        }

        if (config.isCheckIpv6()) {
            Pattern p = config.getCustomIpv6Pattern() != null ? config.getCustomIpv6Pattern() : IPV6;
            MaskUtil.applyMatcher(p.matcher(raw), mask);
        }

        if (config.isCheckDomains() || config.isCheckUrls()) {
            Pattern p = config.getCustomDomainUrlPattern() != null ? config.getCustomDomainUrlPattern() : DOMAIN_URL;
            Matcher m = p.matcher(raw);
            while (m.find()) {
                String match = m.group().toLowerCase();
                if (isWhitelisted(match, config.getWhitelistedDomains())) continue;
                String type = resolveType(match);
                if (type.equals("url") && !config.isCheckUrls()) continue;
                if (type.equals("domain") && !config.isCheckDomains()) continue;
                MaskUtil.applyRange(mask, m.start(), m.end());
            }
        }

        String result = CensorUtil.applyMask(raw, mask, censorChar);
        return result != null ? result : CensorUtil.censorFull(raw, censorChar);
    }

    private static @NonNull String normalise(@NonNull String raw) {
        String s = ZERO_WIDTH.matcher(raw).replaceAll("");
        s = DOT_WORD.matcher(s).replaceAll(".");
        s = COLON_WORD.matcher(s).replaceAll(":");
        s = SPACE_AROUND_PUNCT.matcher(s).replaceAll("$1");
        return s;
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