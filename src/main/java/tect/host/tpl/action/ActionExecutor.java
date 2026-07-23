package tect.host.tpl.action;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.action.condition.ConditionEvaluator;
import tect.host.tpl.action.handler.ActionHandler;
import tect.host.tpl.action.handler.ActionHandlerRegistry;
import tect.host.tpl.action.model.ActionNode;
import tect.host.tpl.action.parser.ActionParser;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;
import tect.host.tpl.util.Utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class ActionExecutor {

    private static final int CACHE_MAX_SIZE = 256;

    private static final int MAX_LOOP_ITERATIONS = 1000;

    private final Map<String, ActionHandler> handlers;
    private final PlaceholderApiHook papi;
    private final Logger logger;

    private final Map<List<String>, CompiledActions> cache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<List<String>, CompiledActions> eldest) {
            return size() > CACHE_MAX_SIZE;
        }
    };

    public ActionExecutor(@NonNull ModuleContext ctx) {
        this.handlers = ActionHandlerRegistry.build(ctx);
        this.papi = ctx.getPlaceholderApiHook();
        this.logger = ctx.getLogger();
    }

    public synchronized @NonNull CompiledActions compile(@NonNull List<String> rawLines) {
        if (rawLines.isEmpty()) return CompiledActions.EMPTY;
        return cache.computeIfAbsent(List.copyOf(rawLines), lines -> new CompiledActions(ActionParser.parse(lines, logger)));
    }

    /**
     * Executes every action in the list for the given player
     * Safe to call from async threads
     */
    public void execute(@NonNull Player player, @NonNull List<String> rawLines) {
        run(player, compile(rawLines).nodes(), null, Map.of());
    }

    public void execute(@NonNull Player player, @NonNull CompiledActions actions) {
        run(player, actions.nodes(), null, Map.of());
    }

    public void execute(@NonNull Player player, @NonNull CompiledActions actions, @NonNull Map<String, String> placeholders) {
        run(player, actions.nodes(), null, placeholders);
    }

    private void run(@NonNull Player player, @NonNull List<ActionNode> nodes, @Nullable String loopVar, @NonNull Map<String, String> placeholders) {
        for (ActionNode node : nodes) {
            switch (node) {
                case ActionNode.Single single -> dispatch(player, single, loopVar, placeholders);
                case ActionNode.Conditional conditional -> runConditional(player, conditional, loopVar, placeholders);
                case ActionNode.Loop loop -> runLoop(player, loop, loopVar, placeholders);
            }
        }
    }

    private void runConditional(@NonNull Player player, ActionNode.@NonNull Conditional conditional, @Nullable String loopVar, @NonNull Map<String, String> placeholders) {
        for (ActionNode.Branch branch : conditional.branches()) {
            String condition = branch.condition();
            if (condition == null) {
                run(player, branch.body(), loopVar, placeholders);
                return;
            }
            if (loopVar != null) condition = condition.replace("{i}", loopVar);
            if (ConditionEvaluator.evaluate(player, papi, condition)) {
                run(player, branch.body(), loopVar, placeholders);
                return;
            }
        }
    }

    private void runLoop(@NonNull Player player, ActionNode.@NonNull Loop loop, @Nullable String outerLoopVar, @NonNull Map<String, String> placeholders) {
        String countExpr = outerLoopVar != null ? loop.countExpr().replace("{i}", outerLoopVar) : loop.countExpr();
        String resolved = papi.apply(player, countExpr.replace("{player}", player.getName())).strip();

        int count;
        try {
            count = Integer.parseInt(resolved);
        } catch (NumberFormatException e) {
            Utils.log(logger, "WARNING", "ActionExecutor [FOR]: invalid count '%s' (resolved '%s')".formatted(loop.countExpr(), resolved));
            return;
        }

        if (count <= 0) return;
        if (count > MAX_LOOP_ITERATIONS) {
            Utils.log(logger, "WARNING", "ActionExecutor [FOR]: count %d exceeds max %d, clamping".formatted(count, MAX_LOOP_ITERATIONS));
            count = MAX_LOOP_ITERATIONS;
        }

        for (int i = 1; i <= count; i++) {
            run(player, loop.body(), String.valueOf(i), placeholders);
        }
    }

    private void dispatch(@NonNull Player player, ActionNode.@NonNull Single single, @Nullable String loopVar, @NonNull Map<String, String> placeholders) {
        ActionHandler handler = handlers.get(single.type());
        if (handler == null) {
            Utils.log(logger, "WARNING", "ActionExecutor: unknown action type [%s]".formatted(single.type()));
            return;
        }
        String arg = single.arg();
        if (!placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                arg = arg.replace(entry.getKey(), entry.getValue());
            }
        }
        if (loopVar != null) arg = arg.replace("{i}", loopVar);
        handler.execute(player, arg);
    }
}