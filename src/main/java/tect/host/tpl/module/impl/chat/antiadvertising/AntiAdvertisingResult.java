package tect.host.tpl.module.impl.chat.antiadvertising;

import org.jspecify.annotations.Nullable;

public record AntiAdvertisingResult(boolean matched, @Nullable String matchType) {
    public static final AntiAdvertisingResult CLEAN = new AntiAdvertisingResult(false, null);
}