package tect.host.tpl.util;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.context.MessageContext;

public final class TagResolvers {

    private TagResolvers() {}

    public static @NonNull TagResolver message(@NonNull MessageContext ctx) {
        return Placeholder.component("message", ctx.getMessage());
    }
}