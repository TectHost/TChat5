package tect.host.tpl.module.impl.chat.blockchat;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.ChatModule;

public final class BlockChatModule implements ChatModule {

    static final String ID = "block-chat";

    private static final String BYPASS_PERMISSION = "tchat.admin.bypass.blockchat";

    private final ModuleContext moduleContext;

    private boolean blocked = false;

    public BlockChatModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        blocked = false;
    }

    @Override
    public void onDisable() {
        blocked = false;
    }

    @Override
    public void process(@NonNull MessageContext msgCtx) {
        if (!blocked) return;
        if (msgCtx.getPlayer().hasPermission(BYPASS_PERMISSION)) return;

        moduleContext.getMessagesManager().sendMessage(msgCtx.getPlayer(), "block-chat-blocked");
        msgCtx.setCancelled(true);
    }

    @Override
    public @NonNull String getId() { return ID; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public void toggle() { this.blocked = !this.blocked; }
}