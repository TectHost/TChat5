package tect.host.tpl.module.impl.chat.chatplaceholders.tag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.module.impl.chat.chatplaceholders.ChatTag;
import tect.host.tpl.util.ColorUtil;

import java.util.Locale;

public final class ItemChatTag implements ChatTag {

    private final ItemChatTagConfig config;

    public ItemChatTag(@NonNull ItemChatTagConfig config) {
        this.config = config;
    }

    @Override
    public @NonNull String getTrigger() { return config.getTrigger(); }

    @Override
    public @NonNull String getPermission() { return config.getPermission(); }

    @Override
    public @Nullable Component resolve(@NonNull Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();

        if (held.getType() == Material.AIR) {
            if (!config.isEmptyHandEnabled()) return null;
            return ColorUtil.deserialize(config.getEmptyHandFormat());
        }

        String name = resolveItemName(held);
        int count = held.getAmount();

        Component label = ColorUtil.deserialize(
                config.getFormatTemplate(),
                Placeholder.parsed("item_name", name),
                Placeholder.unparsed("item_count", String.valueOf(count))
        );

        return label.hoverEvent(held.asHoverEvent());
    }

    private static @NonNull String resolveItemName(@NonNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            Component name = meta.displayName();
            if (name != null) return ColorUtil.toPlainText(name);
        }

        String plain = ColorUtil.toPlainText(item.displayName());
        if (plain.isBlank() || plain.startsWith("item.") || plain.startsWith("block.")) {
            return formatMaterialName(item.getType());
        }
        return plain;
    }

    private static @NonNull String formatMaterialName(@NonNull Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
        }
        return sb.toString();
    }
}