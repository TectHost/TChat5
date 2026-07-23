package tect.host.tpl.util.text;

import org.jspecify.annotations.NonNull;

import java.util.regex.Pattern;

public final class UrlPatterns {

    private UrlPatterns() {}

    public static final @NonNull Pattern DOMAIN_URL = Pattern.compile(
            "(?:https?://|s?ftp://|www\\.)?" +
                    "(?:[a-z0-9](?:[a-z0-9\\-]{0,61}[a-z0-9])?\\.)+" +
                    "[a-z]{2,}" +
                    "(?:/\\S*)?",
            Pattern.CASE_INSENSITIVE
    );
}