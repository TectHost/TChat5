package tect.host.tpl.action.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.action.handler.ActionCategory;
import tect.host.tpl.action.handler.ActionHandler;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;
import tect.host.tpl.util.ColorUtil;

import java.util.Map;

public final class MessagingActions implements ActionCategory {

    @Override
    public void register(@NonNull Map<String, ActionHandler> map, @NonNull ModuleContext ctx) {
        final PlaceholderApiHook papi = ctx.getPlaceholderApiHook();

        map.put("MESSAGE", (p, arg) -> p.sendMessage(ColorUtil.render(papi, p, arg.replace("{player}", p.getName()))));
        map.put("PRINT", (p, arg) -> Bukkit.getServer().broadcast(ColorUtil.render(papi, p, arg.replace("{player}", p.getName()))));
        map.put("BROADCAST", (p, arg) -> {
            for (Player player : ctx.getOnlinePlayers()) {
                player.sendMessage(ColorUtil.render(papi, p, arg.replace("{player}", p.getName())));
            }
        });
    }
}