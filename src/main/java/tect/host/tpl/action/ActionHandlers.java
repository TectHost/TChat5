package tect.host.tpl.action;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.SchedulerAccess;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;
import tect.host.tpl.util.ColorUtil;
import tect.host.tpl.util.Utils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

final class ActionHandlers {

    private ActionHandlers() {}

    static @NonNull @Unmodifiable Map<String, ActionHandler> build(@NonNull ModuleContext ctx) {
        final SchedulerAccess scheduler = ctx.getScheduler();
        final PlaceholderApiHook papi = ctx.getPlaceholderApiHook();
        final Logger log = ctx.getLogger();

        Map<String, ActionHandler> map = new HashMap<>(32);

        // Messaging
        map.put("MESSAGE", (p, arg) -> p.sendMessage(render(papi, p, arg)));
        map.put("BROADCAST", (p, arg) -> Bukkit.getServer().broadcast(render(papi, p, arg)));

        // Debug
        map.put("DEBUG", (_, arg) -> {
            int space = arg.indexOf(' ');
            if (space == -1) { log.info(arg); return; }
            String level = arg.substring(0, space).toUpperCase();
            String message = arg.substring(space + 1);
            Utils.log(log, level, message);
        });

        // Visual
        map.put("TITLE", (p, arg) -> {
            String[] parts = arg.split(";", 2);
            Component title = render(papi, p, parts[0]);
            Component subtitle = parts.length > 1 ? render(papi, p, parts[1]) : Component.empty();
            Title adventureTitle = Title.title(title, subtitle, Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000)));
            scheduler.runSync(() -> p.showTitle(adventureTitle));
        });

        map.put("ACTION_BAR", (p, arg) -> p.sendActionBar(render(papi, p, arg)));

        // Commands
        map.put("PLAYER_COMMAND", (p, arg) -> {
            String cmd = replacePlaceholders(papi, p, arg);
            scheduler.runSync(() -> p.performCommand(cmd));
        });
        map.put("CONSOLE_COMMAND", (p, arg) -> {
            String cmd = replacePlaceholders(papi, p, arg);
            scheduler.runSync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
        });

        // World
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
            double x = parseDouble(parts[1], arg, log);
            double y = parseDouble(parts[2], arg, log);
            double z = parseDouble(parts[3], arg, log);
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) return;
            Location dest = new Location(world, x, y, z);
            scheduler.runSync(() -> p.teleport(dest));
        });

        // Inventory
        map.put("INVENTORY", (p, arg) -> {
            String[] parts = arg.split(" ");
            if (parts.length < 1) return;
            switch (parts[0].toUpperCase()) {
                case "ADD" -> {
                    if (parts.length < 3) { log.warning("ActionExecutor [INVENTORY]: invalid ADD '%s'".formatted(arg)); return; }
                    Material mat = parseMaterial(parts[1], log); if (mat == null) return;
                    int amount = parseInt(parts[2], arg, log); if (amount < 0) return;
                    scheduler.runSync(() -> p.getInventory().addItem(new ItemStack(mat, amount)));
                }
                case "REMOVE" -> {
                    if (parts.length < 3) { log.warning("ActionExecutor [INVENTORY]: invalid REMOVE '%s'".formatted(arg)); return; }
                    Material mat = parseMaterial(parts[1], log); if (mat == null) return;
                    int amount = parseInt(parts[2], arg, log); if (amount < 0) return;
                    scheduler.runSync(() -> p.getInventory().removeItem(new ItemStack(mat, amount)));
                }
                case "CHANGE" -> {
                    if (parts.length < 5) { log.warning("ActionExecutor [INVENTORY]: invalid CHANGE '%s'".formatted(arg)); return; }
                    Material oldMat = parseMaterial(parts[1], log); if (oldMat == null) return;
                    int oldAmount = parseInt(parts[2], arg, log); if (oldAmount < 0) return;
                    Material newMat = parseMaterial(parts[3], log); if (newMat == null) return;
                    int newAmount = parseInt(parts[4], arg, log); if (newAmount < 0) return;
                    scheduler.runSync(() -> {
                        p.getInventory().removeItem(new ItemStack(oldMat, oldAmount));
                        p.getInventory().addItem(new ItemStack(newMat, newAmount));
                    });
                }
                default -> log.warning("ActionExecutor [INVENTORY]: unknown operation '%s'".formatted(parts[0]));
            }
        });

        // Potion
        map.put("POTION_EFFECT", (p, arg) -> {
            String[] parts = arg.split(" ", 2);
            if (parts.length < 2) { log.warning("ActionExecutor [POTION_EFFECT]: invalid format '%s'".formatted(arg)); return; }
            switch (parts[0].toUpperCase()) {
                case "ADD" -> {
                    String[] ep = parts[1].split(":");
                    if (ep.length < 3) { log.warning("ActionExecutor [POTION_EFFECT] ADD: expected TYPE:duration:amplifier, got '%s'".formatted(parts[1])); return; }
                    PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(ep[0].toLowerCase()));
                    if (type == null) { log.warning("ActionExecutor [POTION_EFFECT] ADD: unknown effect '%s'".formatted(ep[0])); return; }
                    int duration = parseInt(ep[1], arg, log); if (duration < 0) return;
                    int amplifier = parseInt(ep[2], arg, log); if (amplifier < 0) return;
                    PotionEffect effect = new PotionEffect(type, duration, amplifier);
                    scheduler.runSync(() -> p.addPotionEffect(effect));
                }
                case "REMOVE" -> {
                    PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(parts[1].toLowerCase()));
                    if (type == null) { log.warning("ActionExecutor [POTION_EFFECT] REMOVE: unknown effect '%s'".formatted(parts[1])); return; }
                    scheduler.runSync(() -> p.removePotionEffect(type));
                }
                default -> log.warning("ActionExecutor [POTION_EFFECT]: unknown operation '%s'".formatted(parts[0]));
            }
        });

        return Map.copyOf(map);
    }

    private static @NonNull Component render(@NonNull PlaceholderApiHook papi, @NonNull Player player, @NonNull String text) {
        return ColorUtil.deserialize(ColorUtil.legacyToMiniSafe(replacePlaceholders(papi, player, text)));
    }

    private static @NonNull String replacePlaceholders(@NonNull PlaceholderApiHook papi, @NonNull Player player, @NonNull String text) {
        return papi.apply(player, text.replace("{player}", player.getName()));
    }

    private static @Nullable Material parseMaterial(@NonNull String name, @NonNull Logger log) {
        Material mat = Material.matchMaterial(name.toUpperCase());
        if (mat == null) log.warning("ActionExecutor [INVENTORY]: unknown material '%s'".formatted(name));
        return mat;
    }

    private static int parseInt(@NonNull String value, @NonNull String fullArg, @NonNull Logger log) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warning("ActionExecutor: expected integer, got '%s' in '%s'".formatted(value, fullArg));
            return -1;
        }
    }

    private static double parseDouble(@NonNull String value, @NonNull String fullArg, @NonNull Logger log) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warning("ActionExecutor: expected number, got '%s' in '%s'".formatted(value, fullArg));
            return Double.NaN;
        }
    }
}