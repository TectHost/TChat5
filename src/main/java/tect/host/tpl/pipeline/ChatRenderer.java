package tect.host.tpl.pipeline;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;
import tect.host.tpl.util.ColorUtil;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.util.TagResolvers;

public final class ChatRenderer {

    private ChatRenderer() {}

    public static @NonNull Component render(@NonNull PlaceholderApiHook hook, @Nullable Player player, @NonNull String template, @NonNull MessageContext ctx) {
        String withPapi = hook.apply(player, template);
        String withLegacy = ColorUtil.legacyToMiniSafe(withPapi);
        return ColorUtil.deserialize(withLegacy, TagResolvers.message(ctx));
    }
}