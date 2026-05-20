package tect.host.tpl.module.type;

import tect.host.tpl.module.Module;
import tect.host.tpl.context.JoinContext;

public interface JoinModule extends Module {
    void onJoin(JoinContext ctx);
}