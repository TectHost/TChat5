package tect.host.tpl.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;

public final class ChatRenderer {

    private ChatRenderer() {}

    /**
     * Renders a chat format template into a final Component.
     *.
     * Pipeline:
     * 1. PAPI placeholders resolved on the raw String
     * 2. Legacy '&' codes + MiniMessage tags converted to MiniMessage String
     * 3. Single MiniMessage parse with <message> tag injected
     */
    public static @NonNull Component render(@NonNull PlaceholderApiHook hook, @Nullable Player player, @NonNull String template, @NonNull MessageContext ctx) {
        String withPlaceholders = hook.apply(player, template);
        String miniString = ColorUtil.legacyToMini(withPlaceholders);
        return ColorUtil.deserialize(miniString, messageTag(ctx));
    }

    private static @NonNull TagResolver messageTag(@NonNull MessageContext ctx) {
        return Placeholder.component("message", ctx.getMessage());
    }
}