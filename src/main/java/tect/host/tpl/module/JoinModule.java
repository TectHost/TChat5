package tect.host.tpl.module;

import tect.host.tpl.util.JoinContext;

public interface JoinModule extends Module {
    void onJoin(JoinContext ctx);
}