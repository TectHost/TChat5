package tect.host.tpl.module.impl.chat.format;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigManager;

public final class FormatModuleConfig {

    private final String formatTemplate;

    public FormatModuleConfig(@NonNull ConfigManager coreConfig) {
        this.formatTemplate = coreConfig.getString("chat.format");
    }

    public @NonNull String getFormatTemplate() {
        return formatTemplate;
    }
}
