package tect.host.tpl.menu;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NonNull;

public interface MenuHolder extends InventoryHolder {
    void handleClick(@NonNull InventoryClickEvent event);

    default void handleClose(@NonNull InventoryCloseEvent event) {}

    default void handleDrag(@NonNull InventoryDragEvent event) {
        event.setCancelled(true);
    }

    @Override
    @NonNull Inventory getInventory();
}