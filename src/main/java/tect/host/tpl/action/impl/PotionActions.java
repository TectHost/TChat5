package tect.host.tpl.action.impl;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.action.handler.ActionCategory;
import tect.host.tpl.action.handler.ActionHandler;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.SchedulerAccess;
import tect.host.tpl.util.Utils;

import java.util.Map;
import java.util.logging.Logger;

public final class PotionActions implements ActionCategory {

    @Override
    public void register(@NonNull Map<String, ActionHandler> map, @NonNull ModuleContext ctx) {
        final SchedulerAccess scheduler = ctx.getScheduler();
        final Logger log = ctx.getLogger();

        map.put("POTION_EFFECT", (p, arg) -> {
            String[] parts = arg.split(" ", 2);
            if (parts.length < 2) { log.warning("ActionExecutor [POTION_EFFECT]: invalid format '%s'".formatted(arg)); return; }
            switch (parts[0].toUpperCase()) {
                case "ADD" -> {
                    String[] ep = parts[1].split(":");
                    if (ep.length < 3) { log.warning("ActionExecutor [POTION_EFFECT] ADD: expected TYPE:duration:amplifier, got '%s'".formatted(parts[1])); return; }
                    PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(ep[0].toLowerCase()));
                    if (type == null) { log.warning("ActionExecutor [POTION_EFFECT] ADD: unknown effect '%s'".formatted(ep[0])); return; }
                    int duration = Utils.parseInt(ep[1], arg, log); if (duration < 0) return;
                    int amplifier = Utils.parseInt(ep[2], arg, log); if (amplifier < 0) return;
                    PotionEffect effect = new PotionEffect(type, duration, amplifier);
                    scheduler.runSync(p, () -> p.addPotionEffect(effect));
                }
                case "REMOVE" -> {
                    PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(parts[1].toLowerCase()));
                    if (type == null) { log.warning("ActionExecutor [POTION_EFFECT] REMOVE: unknown effect '%s'".formatted(parts[1])); return; }
                    scheduler.runSync(p, () -> p.removePotionEffect(type));
                }
                default -> log.warning("ActionExecutor [POTION_EFFECT]: unknown operation '%s'".formatted(parts[0]));
            }
        });
    }
}