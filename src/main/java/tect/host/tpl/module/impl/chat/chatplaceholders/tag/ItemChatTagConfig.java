package tect.host.tpl.module.impl.chat.chatplaceholders.tag;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;

public final class ItemChatTagConfig {

    private final String trigger;
    private final String permission;
    private final String formatTemplate;
    private final boolean emptyHandEnabled;
    private final String emptyHandFormat;

    public ItemChatTagConfig(@NonNull ConfigFile configFile) {
        var cfg = configFile.get();

        this.trigger = cfg.getString("trigger", "[item]");
        this.permission = cfg.getString("permission", "");
        this.formatTemplate = cfg.getString("format", "<aqua><item_name></aqua>");
        this.emptyHandEnabled = cfg.getBoolean("empty-hand.enabled", true);
        this.emptyHandFormat = cfg.getString("empty-hand.format", "[<gray>Air</gray>]");
    }

    public @NonNull String getTrigger() { return trigger; }
    public @NonNull String getPermission() { return permission; }
    public @NonNull String getFormatTemplate() { return formatTemplate; }
    public boolean isEmptyHandEnabled() { return emptyHandEnabled; }
    public @NonNull String getEmptyHandFormat() { return emptyHandFormat; }
}