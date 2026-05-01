package tect.host.tpl.manager;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.module.JoinModule;
import tect.host.tpl.util.JoinContext;

public final class JoinProcessor {

    private final ModuleManager moduleManager;

    public JoinProcessor(@NonNull ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public void process(@NonNull JoinContext ctx) {
        for (JoinModule module : moduleManager.getJoinModules()) {
            if (ctx.isCancelled()) return;
            module.onJoin(ctx);
        }
    }
}