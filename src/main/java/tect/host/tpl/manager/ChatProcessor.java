package tect.host.tpl.manager;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.module.ChatModule;
import tect.host.tpl.module.ModulePhase;
import tect.host.tpl.util.MessageContext;

import java.util.List;

public final class ChatProcessor {

    private static final List<ModulePhase> PHASES = List.of(ModulePhase.values());
    private final ModuleManager moduleManager;

    public ChatProcessor(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public void process(@NonNull MessageContext ctx) {
        for (ModulePhase phase : PHASES) {
            for (ChatModule module : moduleManager.getModulesForPhase(phase)) {
                if (ctx.isCancelled()) return;
                module.process(ctx);
            }
        }
    }
}