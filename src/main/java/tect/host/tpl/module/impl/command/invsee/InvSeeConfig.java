package tect.host.tpl.module.impl.command.invsee;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.util.ColorUtil;

import java.util.List;

public final class InvSeeConfig {

    private final String titleTemplate;
    private final @Nullable ItemStack fillerItem;
    private final @Nullable ItemStack closeItem;

    public InvSeeConfig(@NonNull ConfigFile configFile) {
        var cfg = configFile.get();

        this.titleTemplate = cfg.getString("title", "<gray><player>'s inventory</gray>");

        this.fillerItem = cfg.getBoolean("filler.enabled", true) ? buildItem(cfg.getString("filler.material", "GRAY_STAINED_GLASS_PANE"),
                cfg.getString("filler.name", " "),
                cfg.getStringList("filler.lore")) : null;

        this.closeItem = cfg.getBoolean("close-button.enabled", true) ? buildItem(cfg.getString("close-button.material", "BARRIER"),
                cfg.getString("close-button.name", "<red>Close"),
                cfg.getStringList("close-button.lore")) : null;
    }

    private static @Nullable ItemStack buildItem(@Nullable String materialName, @Nullable String name, @NonNull List<String> lore) {
        if (materialName == null) return null;

        Material material = Material.matchMaterial(materialName);
        if (material == null || material == Material.AIR) return null;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (name != null) {
            meta.displayName(ColorUtil.deserialize(name));
        }

        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(ColorUtil::deserialize).toList());
        }

        item.setItemMeta(meta);
        return item;
    }

    public @NonNull Component resolveTitle(@NonNull String playerName) {
        return ColorUtil.deserialize(titleTemplate, Placeholder.unparsed("player", playerName));
    }
    public @Nullable ItemStack getFillerItem() { return fillerItem; }
    public @Nullable ItemStack getCloseItem() { return closeItem; }
}