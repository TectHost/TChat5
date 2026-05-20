package tect.host.tpl.module.type;

import tect.host.tpl.module.Module;
import tect.host.tpl.context.CommandContext;

public interface CommandModule extends Module {
    void process(CommandContext ctx);
}