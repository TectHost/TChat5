package tect.host.tpl.module.impl.command.customcommands;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigFile;

import java.util.List;

public final class CustomCommandConfig {

    private CustomCommandConfig() {}

    public static @NonNull CustomCommandDefinition parse(@NonNull String commandName, @NonNull ConfigFile configFile) {
        var cfg = configFile.get();

        String name = sanitize(cfg.getString("name", commandName));
        List<String> aliases = cfg.getStringList("aliases").stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::strip)
                .toList();

        ConfigurationSection argsSec = cfg.getConfigurationSection("args");
        int minArgs = argsSec != null ? Math.max(0, argsSec.getInt("min", 0)) : 0;
        int maxArgs = argsSec != null ? argsSec.getInt("max", -1) : -1;
        String usage = argsSec != null ? argsSec.getString("usage", "/" + name) : "/" + name;
        List<String> usageActions = argsSec != null ? filterActions(argsSec.getStringList("usage-actions")) : List.of();

        String rawPerm = cfg.getString("permission", "");
        String permission = rawPerm.isBlank() ? null : rawPerm.strip();
        List<String> noPermActions = filterActions(cfg.getStringList("no-permission-actions"));

        int cooldown = Math.max(0, cfg.getInt("cooldown", 0));
        List<String> cooldownActions = filterActions(cfg.getStringList("cooldown-actions"));

        List<String> actions = filterActions(cfg.getStringList("actions"));

        return new CustomCommandDefinition(
                name, aliases,
                minArgs, maxArgs,
                permission, noPermActions,
                cooldown, cooldownActions,
                usageActions, usage,
                actions
        );
    }

    private static @NonNull String sanitize(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return "unknown";
        String s = raw.strip();
        return s.startsWith("/") ? s.substring(1) : s;
    }

    private static @NonNull @Unmodifiable List<String> filterActions(@NonNull List<String> raw) {
        return raw.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::strip)
                .toList();
    }
}