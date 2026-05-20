package tect.host.tpl.module.impl.chat.colorchat;

import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.util.ColorUtil;
import tect.host.tpl.context.MessageContext;

/**
 * Pipeline position:
 *   Runs first in PRE_PROCESS so downstream modules (FormatModule, GroupModule)
 *   always receive an already-sanitized, already-parsed Component via ctx.getMessage()
 *.
 * Flow:
 *   1. Cheap early-exit if rawMessage contains no markup ('&' or '<')
 *   2. Sanitize: translate allowed legacy codes to MiniMessage tags, strip the rest
 *   3. Cancel if the result is blank (e.g. player sent only "<green>" or "&a")
 *   4. Parse the sanitized MiniMessage string into a Component (single parse)
 *   5. Write the Component back to the messages context
 */
public final class ColorChatModule implements ChatModule {

    private static final String ID = "colorchat";

    public ColorChatModule(@NonNull ModuleContext moduleContext) {}

    @Override public @NonNull String getId() { return ID; }

    /* This module does not have onEnable, onDisable, or onReload, since it does not need to execute anything */

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