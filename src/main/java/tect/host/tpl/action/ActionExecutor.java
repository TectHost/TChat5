package tect.host.tpl.action;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.util.Utils;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class ActionExecutor {

    private final Map<String, ActionHandler> handlers;
    private final Logger logger;

    public ActionExecutor(@NonNull ModuleContext ctx) {
        this.handlers = ActionHandlers.build(ctx);
        this.logger = ctx.getLogger();
    }

    /**
     * Executes every action in the list for the given player
     * Safe to call from async threads
     */
    public void execute(@NonNull Player player, @NonNull List<String> actions) {
        for (String raw : actions) {
            if (raw == null || raw.isBlank()) continue;
            dispatch(player, raw.strip());
        }
    }

    private void dispatch(@NonNull Player player, @NonNull String raw) {
        if (raw.charAt(0) != '[') { warn(raw); return; }
        int close = raw.indexOf(']');
        if (close < 2) { warn(raw); return; }

        String type = raw.substring(1, close).toUpperCase();
        String arg  = close + 1 < raw.length() ? raw.substring(close + 1).stripLeading() : "";

        ActionHandler handler = handlers.get(type);
        if (handler == null) {
            Utils.log(logger, "WARNING", "ActionExecutor: unknown action type [%s]".formatted(type));
            return;
        }
        handler.execute(player, arg);
    }

    private void warn(@NonNull String raw) {
        Utils.log(logger, "WARNING", "ActionExecutor: malformed action (missing [TYPE]): %s".formatted(raw));
    }
}