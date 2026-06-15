package tect.host.tpl.module.impl.chat.chatplaceholders;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;

public final class ChatPlaceholdersConfig {

    private final int maxReplacementsPerMessage;
    private final boolean itemEnabled;

    public ChatPlaceholdersConfig(@NonNull ConfigFile configFile) {
        var cfg = configFile.get();
        this.maxReplacementsPerMessage = cfg.getInt("max-replacements-per-message", 5);
        this.itemEnabled = cfg.getBoolean("built-in.item", true);
    }

    public int getMaxReplacementsPerMessage() { return maxReplacementsPerMessage; }

    public boolean isItemEnabled() { return itemEnabled; }
}