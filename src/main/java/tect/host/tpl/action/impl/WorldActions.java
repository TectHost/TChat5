package tect.host.tpl.action.impl;

import org.bukkit.*;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.action.handler.ActionCategory;
import tect.host.tpl.action.handler.ActionHandler;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.SchedulerAccess;
import tect.host.tpl.util.Utils;

import java.util.Map;
import java.util.logging.Logger;

public final class WorldActions implements ActionCategory {

    @Override
    public void register(@NonNull Map<String, ActionHandler> map, @NonNull ModuleContext ctx) {
        final SchedulerAccess scheduler = ctx.getScheduler();
        final Logger log = ctx.getLogger();

        map.put("SOUND", (p, arg) -> {
            Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(arg.toLowerCase()));
            if (sound == null) { log.warning("ActionExecutor [SOUND]: unknown sound '%s'".formatted(arg)); return; }
            scheduler.runSync(() -> p.playSound(p.getLocation(), sound, 1f, 1f));
        });
        map.put("PARTICLE", (p, arg) -> {
            Particle particle = Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(arg.toLowerCase()));
            if (particle == null) { log.warning("ActionExecutor [PARTICLE]: unknown particle '%s'".formatted(arg)); return; }
            scheduler.runSync(() -> p.getWorld().spawnParticle(particle, p.getLocation(), 10, null));
        });
        map.put("TELEPORT", (p, arg) -> {
            String[] parts = arg.split(";");
            if (parts.length < 4) {
                log.warning("ActionExecutor [TELEPORT]: invalid format '%s', expected world;x;y;z".formatted(arg));
                return;
            }
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                log.warning("ActionExecutor [TELEPORT]: world '%s' not found".formatted(parts[0]));
                return;
            }
            double x = Utils.parseDouble(parts[1], arg, log);
            double y = Utils.parseDouble(parts[2], arg, log);
            double z = Utils.parseDouble(parts[3], arg, log);
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) return;
            Location dest = new Location(world, x, y, z);
            scheduler.runSync(() -> p.teleport(dest));
        });
    }
}