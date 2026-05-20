package tect.host.tpl.module.impl.chat.format;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.pipeline.ChatRenderer;
import tect.host.tpl.context.MessageContext;

public class FormatModule implements ChatModule {

    private final ModuleContext moduleContext;

    private String formatTemplate = "";

    public FormatModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        load();
    }

    @Override
    public void onReload() {
        load();
    }

    private void load() {
        FormatModuleConfig config = new FormatModuleConfig(moduleContext.getCoreConfig());
        this.formatTemplate = config.getFormatTemplate();
    }

    @Override
    public void process(@NonNull MessageContext ctx) {
        if (ctx.getFormat() != null || formatTemplate.isBlank()) return;

        ctx.setFormat(ChatRenderer.render(
                moduleContext.getPlaceholderApiHook(), ctx.getPlayer(), formatTemplate, ctx
        ));
    }

    @Override
    public @NonNull String getId() { return "format"; }
}