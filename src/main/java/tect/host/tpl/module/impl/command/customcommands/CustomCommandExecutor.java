package tect.host.tpl.module.impl.command.customcommands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.action.ActionExecutor;
import tect.host.tpl.module.ModuleCommand;
import tect.host.tpl.util.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomCommandExecutor implements ModuleCommand {

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private final CustomCommandDefinition def;
    private final ActionExecutor actionExecutor;

    public CustomCommandExecutor(@NonNull CustomCommandDefinition def, @NonNull ActionExecutor actionExecutor) {
        this.def = def;
        this.actionExecutor = actionExecutor;
    }

    @Override
    public @NonNull String getName() {
        return def.name();
    }

    @Override
    public @NonNull List<String> getAliases() {
        return def.aliases();
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, String @NonNull [] args) {
        if (!(source.getSender() instanceof Player player)) return;

        // Permission check
        if (def.permission() != null && !Utils.hasPerms(player, def.permission())) {
            actionExecutor.execute(player, def.noPermissionActions());
            return;
        }

        // Cooldown check
        if (def.cooldownSeconds() > 0) {
            long nowMs = System.currentTimeMillis();
            Long lastMs = cooldowns.get(player.getUniqueId());
            if (lastMs != null) {
                long elapsedMs = nowMs - lastMs;
                long cooldownMs = (long) def.cooldownSeconds() * 1_000L;
                if (elapsedMs < cooldownMs) {
                    long remainingSecs = (long) Math.ceil((cooldownMs - elapsedMs) / 1_000.0);
                    List<String> resolved = resolveCooldownPlaceholders(def.cooldownActions(), remainingSecs);
                    actionExecutor.execute(player, resolved);
                    return;
                }
            }
        }

        // Arg count validation
        int provided = args.length;
        if (provided < def.minArgs()) {
            List<String> resolved = resolveUsagePlaceholders(def.usageActions(), def.usage());
            actionExecutor.execute(player, resolved);
            return;
        }

        String[] effectiveArgs = (def.maxArgs() >= 0 && provided > def.maxArgs()) ? Arrays.copyOf(args, def.maxArgs()) : args;

        // Register cooldown timestamp BEFORE executing actions
        if (def.cooldownSeconds() > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }

        // Resolve arg placeholders in actions and execute
        List<String> resolved = resolveArgPlaceholders(def.actions(), effectiveArgs);
        actionExecutor.execute(player, resolved);
    }

    private static @NonNull List<String> resolveArgPlaceholders(@NonNull List<String> actions, String @NonNull [] args) {
        if (actions.isEmpty()) return actions;

        String allArgs = String.join(" ", args);

        return actions.stream()
                .map(action -> {
                    String s = action.replace("{args}", allArgs);
                    for (int i = 0; i < args.length; i++) {
                        s = s.replace("{arg" + i + "}", args[i]);
                    }
                    return s;
                })
                .toList();
    }

    private static @NonNull List<String> resolveCooldownPlaceholders(@NonNull List<String> actions, long remainingSecs) {
        if (actions.isEmpty()) return actions;
        String remaining = String.valueOf(remainingSecs);
        return actions.stream().map(a -> a.replace("{remaining}", remaining)).toList();
    }

    private static @NonNull List<String> resolveUsagePlaceholders(@NonNull List<String> actions, @NonNull String usage) {
        if (actions.isEmpty()) return actions;
        return actions.stream().map(a -> a.replace("{usage}", usage)).toList();
    }
}