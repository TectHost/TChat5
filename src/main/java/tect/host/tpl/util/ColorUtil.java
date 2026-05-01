package tect.host.tpl.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;

public final class ColorUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private ColorUtil() {}

    /**
     * Full pipeline to Component for config templates
     * Resolves PAPI then parses as MiniMessage
     */
    public static @NonNull Component translate(@NonNull PlaceholderApiHook hook, @Nullable Player player, @NonNull String message) {
        return deserialize(hook.apply(player, message));
    }

    public static @NonNull String legacyToMini(@NonNull String legacy) {
        return MINI.serialize(LEGACY.deserialize(legacy))
                .replace("\\<", "<")
                .replace("\\>", ">");
    }

    /**
     * Extracts plain text from a Component, stripping all formatting.
     */
    public static @NonNull String toPlainText(@NonNull Component component) {
        return PLAIN.serialize(component);
    }

    public static @NonNull Component deserialize(@NonNull String miniMessage) {
        return MINI.deserialize(miniMessage);
    }

    public static @NonNull Component deserialize(@NonNull String miniMessage, @NonNull TagResolver tagResolver) {
        return MINI.deserialize(miniMessage, tagResolver);
    }
}