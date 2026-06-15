package tect.host.tpl.module.impl.command.invsee;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.menu.MenuHolder;

public final class InvSeeHolder implements MenuHolder {

    private static final int SLOT_CLOSE = 44;
    private static final int SLOTS_TOTAL = 45;
    private static final int FIRST_FREE = 41;

    private final Inventory inventory;

    private final Player target;

    InvSeeHolder(@NonNull Player target, @NonNull InvSeeConfig config) {
        this.target = target;
        this.inventory = Bukkit.createInventory(this, SLOTS_TOTAL, config.resolveTitle(target.getName()));
        populateSlots(config);
    }

    private void populateSlots(@NonNull InvSeeConfig config) {
        // Main inventory + hotbar (slots 0–35)
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, target.getInventory().getItem(i));
        }

        // Armour (slots 36–39: boots, leggings, chestplate, helmet)
        ItemStack[] armour = target.getInventory().getArmorContents();
        for (int i = 0; i < armour.length; i++) {
            inventory.setItem(36 + i, armour[i]);
        }

        // Offhand (slot 40)
        inventory.setItem(40, target.getInventory().getItemInOffHand());

        // Filler for slots 41–43
        ItemStack filler = config.getFillerItem();
        if (filler != null) {
            for (int i = FIRST_FREE; i < SLOT_CLOSE; i++) {
                inventory.setItem(i, filler);
            }
        }

        // Close button (slot 44)
        ItemStack closeItem = config.getCloseItem();
        if (closeItem != null) {
            inventory.setItem(SLOT_CLOSE, closeItem);
        }
    }

    @Override
    public void handleClick(@NonNull InventoryClickEvent event) {
        // Close button
        if (event.getRawSlot() == SLOT_CLOSE) {
            event.getWhoClicked().closeInventory();
            return;
        }

        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT || event.getClick() == ClickType.NUMBER_KEY) {
            event.setCancelled(true);
        }
    }

    public @NonNull Player getTarget() { return target; }

    @Override
    public @NonNull Inventory getInventory() { return inventory; }
}