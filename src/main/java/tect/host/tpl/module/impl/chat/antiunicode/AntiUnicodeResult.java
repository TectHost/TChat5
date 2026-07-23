package tect.host.tpl.module.impl.chat.antiunicode;

import org.jspecify.annotations.Nullable;

public record AntiUnicodeResult(boolean matched, @Nullable String matchType) {
    public static final AntiUnicodeResult CLEAN = new AntiUnicodeResult(false, null);
}