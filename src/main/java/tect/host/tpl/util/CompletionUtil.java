package tect.host.tpl.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class CompletionUtil {

    private CompletionUtil() {}

    public static @NonNull @Unmodifiable List<String> filterOnlinePlayers(@NonNull String partial) {
        String lp = partial.toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lp))
                .toList();
    }

    public static @NonNull @Unmodifiable List<String> filterFrom(@NonNull Collection<String> options, @NonNull String partial) {
        String lp = partial.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lp))
                .toList();
    }
}