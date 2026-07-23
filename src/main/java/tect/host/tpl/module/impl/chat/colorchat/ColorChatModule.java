package tect.host.tpl.module.impl.chat.colorchat;

import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.util.ColorUtil;

public final class ColorChatModule implements ChatModule {

    private static final String ID = "colorchat";

    public ColorChatModule(ModuleContext moduleContext) {}

    @Override public @NonNull String getId() { return ID; }

    @Override
    public void process(@NonNull MessageContext msgCtx) {
        if (msgCtx.isCancelled()) return;

        String rawText = msgCtx.getEffectiveRaw();

        if (!ColorChatParser.hasMarkup(rawText)) {
            if (!msgCtx.getEffectiveRaw().equals(msgCtx.getRawMessage())) {
                msgCtx.setMessage(Component.text(rawText));
            }
            return;
        }

        String sanitized = ColorChatParser.sanitize(msgCtx.getPlayer(), rawText);

        if (ColorChatParser.isBlankAfterTags(sanitized)) {
            msgCtx.setCancelled(true);
            return;
        }

        msgCtx.setMessage(ColorUtil.deserialize(sanitized));
    }
}