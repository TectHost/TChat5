package tect.host.tpl.module.impl.chat.clickablelinks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.util.text.UrlPatterns;

public final class ClickableLinksModule implements ChatModule {

    private static final String ID = "clickable-links";

    public ClickableLinksModule(@NonNull ModuleContext moduleContext) {}

    @Override
    public void process(@NonNull MessageContext ctx) {
        if (ctx.isCancelled()) return;

        Component message = ctx.getMessage();

        Component replaced = message.replaceText(TextReplacementConfig.builder()
                .match(UrlPatterns.DOMAIN_URL)
                .replacement((matchResult, builder) -> {
                    String url = matchResult.group();
                    String target = url.matches("(?i)^https?://.*") ? url : "https://" + url;
                    return builder.clickEvent(ClickEvent.openUrl(target));
                })
                .build());

        if (replaced != message) {
            ctx.setMessage(replaced);
        }
    }

    @Override
    public @NonNull String getId() { return ID; }
}