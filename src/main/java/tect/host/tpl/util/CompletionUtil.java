package tect.host.tpl.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;

public final class CompletionUtil {

    private CompletionUtil() {}

    public static @NonNull @Unmodifiable List<String> filterOnlinePlayers(@NonNull String partial) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                .toList();
    }

    public static @NonNull @Unmodifiable List<String> filterFrom(@NonNull Collection<String> options, @NonNull String partial) {
        String lp = partial.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lp))
                .toList();
    }
}