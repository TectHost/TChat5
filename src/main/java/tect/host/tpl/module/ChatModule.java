package tect.host.tpl.module;

import tect.host.tpl.util.MessageContext;

public interface ChatModule extends Module {
    void process(MessageContext ctx);
}