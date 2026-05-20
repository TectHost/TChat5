package tect.host.tpl.module.type;

import tect.host.tpl.module.Module;
import tect.host.tpl.context.MessageContext;

public interface ChatModule extends Module {
    void process(MessageContext ctx);
}