package tect.host.tpl.module.type;

import tect.host.tpl.module.Module;
import tect.host.tpl.context.QuitContext;

public interface QuitModule extends Module {
    void onQuit(QuitContext ctx);
}