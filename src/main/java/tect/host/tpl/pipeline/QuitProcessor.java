package tect.host.tpl.pipeline;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.module.registry.ModuleManager;
import tect.host.tpl.module.type.QuitModule;
import tect.host.tpl.context.QuitContext;

public final class QuitProcessor {

    private final ModuleManager moduleManager;

    public QuitProcessor(@NonNull ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public void process(@NonNull QuitContext ctx) {
        for (QuitModule module : moduleManager.getQuitModules()) {
            module.onQuit(ctx);
        }
    }
}