package tect.host.tpl.module.impl.chat.chatplaceholders;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface ChatTag {
    @NonNull String getTrigger();

    @Nullable Component resolve(@NonNull Player player);

    default @NonNull String getPermission() { return ""; }
}