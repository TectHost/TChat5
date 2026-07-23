package tect.host.tpl.action.impl;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.action.handler.ActionCategory;
import tect.host.tpl.action.handler.ActionHandler;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.SchedulerAccess;
import tect.host.tpl.util.Utils;

import java.util.Map;
import java.util.logging.Logger;

public final class InventoryActions implements ActionCategory {

    @Override
    public void register(@NonNull Map<String, ActionHandler> map, @NonNull ModuleContext ctx) {
        final SchedulerAccess scheduler = ctx.getScheduler();
        final Logger log = ctx.getLogger();

        map.put("INVENTORY", (p, arg) -> {
            String[] parts = arg.split(" ");
            if (parts.length < 1) return;
            switch (parts[0].toUpperCase()) {
                case "ADD" -> {
                    if (parts.length < 3) { log.warning("ActionExecutor [INVENTORY]: invalid ADD '%s'".formatted(arg)); return; }
                    Material mat = Utils.parseMaterial(parts[1], log); if (mat == null) return;
                    int amount = Utils.parseInt(parts[2], arg, log); if (amount < 0) return;
                    scheduler.runSync(p, () -> p.getInventory().addItem(new ItemStack(mat, amount)));
                }
                case "REMOVE" -> {
                    if (parts.length < 3) { log.warning("ActionExecutor [INVENTORY]: invalid REMOVE '%s'".formatted(arg)); return; }
                    Material mat = Utils.parseMaterial(parts[1], log); if (mat == null) return;
                    int amount = Utils.parseInt(parts[2], arg, log); if (amount < 0) return;
                    scheduler.runSync(p, () -> p.getInventory().removeItem(new ItemStack(mat, amount)));
                }
                case "CHANGE" -> {
                    if (parts.length < 5) { log.warning("ActionExecutor [INVENTORY]: invalid CHANGE '%s'".formatted(arg)); return; }
                    Material oldMat = Utils.parseMaterial(parts[1], log); if (oldMat == null) return;
                    int oldAmount = Utils.parseInt(parts[2], arg, log); if (oldAmount < 0) return;
                    Material newMat = Utils.parseMaterial(parts[3], log); if (newMat == null) return;
                    int newAmount = Utils.parseInt(parts[4], arg, log); if (newAmount < 0) return;
                    scheduler.runSync(p, () -> {
                        p.getInventory().removeItem(new ItemStack(oldMat, oldAmount));
                        p.getInventory().addItem(new ItemStack(newMat, newAmount));
                    });
                }
                default -> log.warning("ActionExecutor [INVENTORY]: unknown operation '%s'".formatted(parts[0]));
            }
        });
    }
}