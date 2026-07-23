package tect.host.tpl.module.impl.command.customcommands;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record CustomCommandDefinition(
        @NonNull String name,

        @NonNull List<String> aliases,

        int minArgs,
        int maxArgs,

        @Nullable String permission,
        @NonNull List<String> noPermissionActions,

        int cooldownSeconds,
        @NonNull List<String> cooldownActions,

        @NonNull List<String> usageActions,
        @NonNull String usage,

        @NonNull List<String> actions
) {}