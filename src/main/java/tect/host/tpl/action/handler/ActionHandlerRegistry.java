package tect.host.tpl.action.handler;

import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.action.impl.*;
import tect.host.tpl.module.ModuleContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ActionHandlerRegistry {

    private ActionHandlerRegistry() {}

    private static final List<ActionCategory> CATEGORIES = List.of(
            new MessagingActions(),
            new VisualActions(),
            new CommandActions(),
            new WorldActions(),
            new InventoryActions(),
            new PotionActions()
    );

    public static @NonNull @Unmodifiable Map<String, ActionHandler> build(@NonNull ModuleContext ctx) {
        Map<String, ActionHandler> map = new HashMap<>(32);
        for (ActionCategory category : CATEGORIES) {
            category.register(map, ctx);
        }
        return Map.copyOf(map);
    }
}