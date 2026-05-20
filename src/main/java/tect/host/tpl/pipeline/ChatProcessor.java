package tect.host.tpl.pipeline;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.module.registry.ModuleManager;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.module.ModulePhase;
import tect.host.tpl.context.MessageContext;

import java.util.EnumSet;
import java.util.List;

public final class ChatProcessor {

    private static final EnumSet<ModulePhase> PHASES = EnumSet.allOf(ModulePhase.class);

    private final ModuleManager moduleManager;

    public ChatProcessor(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public void process(@NonNull MessageContext ctx) {
        for (ModulePhase phase : PHASES) {
            List<ChatModule> modules = moduleManager.getModulesForPhase(phase);
            if (modules.isEmpty()) continue;

            for (ChatModule module : modules) {
                if (ctx.isCancelled()) break;
                module.process(ctx);
            }

            if (ctx.isCancelled()) return;
        }
    }
}