package tect.host.tpl.action.handler;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.module.ModuleContext;

import java.util.Map;

public interface ActionCategory {
    void register(@NonNull Map<String, ActionHandler> map, @NonNull ModuleContext ctx);
}