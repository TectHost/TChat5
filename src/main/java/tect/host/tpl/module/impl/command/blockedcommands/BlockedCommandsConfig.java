package tect.host.tpl.module.impl.command.blockedcommands;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class BlockedCommandsConfig {

    private final Set<String> blockedRoots;
    private final List<String> actions;

    public BlockedCommandsConfig(@NonNull ConfigFile configFile) {
        List<String> raw = configFile.get().getStringList("blocked-commands");
        this.blockedRoots = raw.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.stripLeading().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        List<String> rawActions = configFile.get().getStringList("actions");
        this.actions = rawActions.stream().filter(l -> l != null && !l.isBlank()).toList();
    }

    public @NonNull Set<String> getBlockedRoots() { return blockedRoots; }
    public @NonNull List<String> getActions() { return actions; }
}