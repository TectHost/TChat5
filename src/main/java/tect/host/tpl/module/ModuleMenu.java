package tect.host.tpl.module;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.List;

public interface ModuleMenu {
    @NonNull String getId();

    void open(@NonNull Player player, String @NonNull [] args);

    default @NonNull List<String> getAliases() {
        return List.of();
    }
}