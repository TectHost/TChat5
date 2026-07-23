package tect.host.tpl.module.impl.chat.antispam;

import org.jspecify.annotations.Nullable;

public record AntiSpamResult(boolean matched, @Nullable String matchType) {
    public static final AntiSpamResult CLEAN = new AntiSpamResult(false, null);
}