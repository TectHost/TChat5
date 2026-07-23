package tect.host.tpl.action.impl;

import org.bukkit.Bukkit;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.action.handler.ActionCategory;
import tect.host.tpl.action.handler.ActionHandler;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.SchedulerAccess;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;

import java.util.Map;

public final class CommandActions implements ActionCategory {

    @Override
    public void register(@NonNull Map<String, ActionHandler> map, @NonNull ModuleContext ctx) {
        final SchedulerAccess scheduler = ctx.getScheduler();
        final PlaceholderApiHook papi = ctx.getPlaceholderApiHook();

        map.put("PLAYER_COMMAND", (p, arg) -> {
            String cmd = papi.apply(p, arg.replace("{player}", p.getName()));
            scheduler.runSync(() -> p.performCommand(cmd));
        });
        map.put("CONSOLE_COMMAND", (p, arg) -> {
            String cmd = papi.apply(p, arg.replace("{player}", p.getName()));
            scheduler.runSync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
        });
    }
}