package tect.host.tpl.pipeline;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.module.registry.ModuleManager;
import tect.host.tpl.module.type.CommandModule;
import tect.host.tpl.context.CommandContext;

import java.util.List;

public final class CommandProcessor {

    private final ModuleManager moduleManager;

    public CommandProcessor(@NonNull ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public boolean process(@NonNull CommandContext ctx) {
        List<CommandModule> modules = moduleManager.getCommandModules();

        for (CommandModule module : modules) {
            if (ctx.isCancelled()) break;
            module.process(ctx);
        }

        return !ctx.isCancelled();
    }
}