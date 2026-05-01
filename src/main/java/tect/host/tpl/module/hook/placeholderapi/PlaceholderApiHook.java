package tect.host.tpl.module.hook.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.TChat;
import tect.host.tpl.manager.ModuleManager;

public final class PlaceholderApiHook {

    private final boolean available;
    private @Nullable PlaceholderExpansion registeredExpansion;

    public PlaceholderApiHook() {
        this.available = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public @NonNull String apply(@Nullable Player player, @NonNull String text) {
        if (!available || player == null) return text;
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public void registerExpansion(@NonNull TChat plugin, @NonNull ModuleManager moduleManager) {
        if (!available) return;

        if (registeredExpansion == null) {
            registeredExpansion = new PlaceholderExpansion(plugin, moduleManager);
            registeredExpansion.register();
        }

        registeredExpansion.refreshGroupService();
    }

    public void unregisterExpansion() {
        if (registeredExpansion == null) return;
        registeredExpansion.unregister();
        registeredExpansion = null;
    }
}