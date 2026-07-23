package tect.host.tpl.module.impl.command.commandcooldown;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;

import java.util.List;

public final class CommandCooldownConfig {

    private final long cooldownMillis;
    private final List<String> actions;

    public CommandCooldownConfig(@NonNull ConfigFile configFile) {
        var cfg = configFile.get();

        int seconds = Math.max(0, cfg.getInt("cooldown-seconds", 3));
        this.cooldownMillis = seconds * 1000L;

        this.actions = cfg.getStringList("actions").stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::strip)
                .toList();
    }

    public long getCooldownMillis() { return cooldownMillis; }
    public @NonNull List<String> getActions() { return actions; }
}