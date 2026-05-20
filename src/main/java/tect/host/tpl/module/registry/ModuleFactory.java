package tect.host.tpl.module.registry;

import tect.host.tpl.module.Module;
import tect.host.tpl.module.ModuleContext;

@FunctionalInterface
public interface ModuleFactory {
    Module create(ModuleContext context);
}