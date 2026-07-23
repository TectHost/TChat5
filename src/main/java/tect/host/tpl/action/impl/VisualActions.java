package tect.host.tpl.action.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.action.handler.ActionCategory;
import tect.host.tpl.action.handler.ActionHandler;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.SchedulerAccess;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;
import tect.host.tpl.util.ColorUtil;

import java.time.Duration;
import java.util.Map;

public final class VisualActions implements ActionCategory {

    @Override
    public void register(@NonNull Map<String, ActionHandler> map, @NonNull ModuleContext ctx) {
        final SchedulerAccess scheduler = ctx.getScheduler();
        final PlaceholderApiHook papi = ctx.getPlaceholderApiHook();

        map.put("TITLE", (p, arg) -> {
            String[] parts = arg.split(";", 2);
            Component title = ColorUtil.render(papi, p, parts[0].replace("{player}", p.getName()));
            Component subtitle = parts.length > 1 ? ColorUtil.render(papi, p, parts[1].replace("{player}", p.getName())) : Component.empty();
            Title adventureTitle = Title.title(title, subtitle, Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000)));
            scheduler.runSync(() -> p.showTitle(adventureTitle));
        });

        map.put("ACTION_BAR", (p, arg) -> p.sendActionBar(ColorUtil.render(papi, p, arg)));
    }
}