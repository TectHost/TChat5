package tect.host.tpl.module.impl.chat.chatdelete;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;

public final class ChatDeleteConfig {

    private final String prefixText;
    private final int cacheSize;
    private final long cacheTtlSeconds;

    public ChatDeleteConfig(@NonNull ConfigFile configFile) {
        this.prefixText = configFile.get().getString("prefix-text", "<hover:show_text:'<gray>Delete this message</gray>'><click:run_command:'/chatdelete %id%'><red>[X]</red></click></hover> ");
        this.cacheSize = Math.max(1, configFile.get().getInt("cache-size", 200));
        this.cacheTtlSeconds = Math.max(1, configFile.get().getLong("cache-ttl-seconds", 300L));
    }

    public @NonNull String getPrefixText() { return prefixText; }
    public int getCacheSize() { return cacheSize; }
    public long getCacheTtlSeconds() { return cacheTtlSeconds; }

}