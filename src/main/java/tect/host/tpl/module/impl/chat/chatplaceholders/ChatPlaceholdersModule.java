package tect.host.tpl.module.impl.chat.chatplaceholders;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;
import tect.host.tpl.module.impl.chat.chatplaceholders.custom.CustomTagLoader;
import tect.host.tpl.module.impl.chat.chatplaceholders.engine.TagReplacementEngine;
import tect.host.tpl.module.impl.chat.chatplaceholders.tag.ItemChatTag;
import tect.host.tpl.module.impl.chat.chatplaceholders.tag.ItemChatTagConfig;
import tect.host.tpl.module.type.ChatModule;

import java.util.Map;

public final class ChatPlaceholdersModule implements ChatModule {

    private static final String ID = "chat-placeholders";

    private static final String FOLDER = "modules/chatplaceholders";

    private final ModuleContext ctx;
    private final PlaceholderApiHook placeholderApiHook;

    private ConfigFile mainCfgFile;
    private ConfigFile itemCfgFile;
    private ConfigFile customCfgFile;

    private volatile ChatTagRegistry registry;
    private volatile int maxReplacements = 5;

    public ChatPlaceholdersModule(@NonNull ModuleContext ctx) {
        this.ctx = ctx;
        this.placeholderApiHook = ctx.getPlaceholderApiHook();
        this.registry = new ChatTagRegistry(ctx.getLogger());
    }

    @Override
    public void onEnable() {
        mainCfgFile = ctx.createConfigFile("chatplaceholders.yml", FOLDER);
        itemCfgFile = ctx.createConfigFile("itemtag.yml", FOLDER);
        customCfgFile = ctx.createConfigFile("custom-tags.yml", FOLDER);

        mainCfgFile.register();
        itemCfgFile.register();
        customCfgFile.register();

        loadRegistry();
    }

    @Override
    public void onReload() {
        mainCfgFile.reload();
        itemCfgFile.reload();
        customCfgFile.reload();
        loadRegistry();
    }

    @Override
    public void onDisable() {
        registry.clear();
    }

    private void loadRegistry() {
        ChatPlaceholdersConfig cfg = new ChatPlaceholdersConfig(mainCfgFile);
        this.maxReplacements = cfg.getMaxReplacementsPerMessage();

        ChatTagRegistry reg = new ChatTagRegistry(ctx.getLogger());

        if (cfg.isItemEnabled()) {
            reg.register(new ItemChatTag(new ItemChatTagConfig(itemCfgFile)));
        }

        reg.registerAll(CustomTagLoader.load(customCfgFile, ctx.getActionExecutor(), placeholderApiHook, ctx.getLogger()));

        this.registry = reg;
        ctx.getDebugLogger().info("ChatPlaceholders: loaded %d tag(s).".formatted(reg.size()));
    }

    @Override
    public void process(@NonNull MessageContext msgCtx) {
        Map<String, ChatTag> tags = registry.getTags();
        if (tags.isEmpty()) return;

        msgCtx.setMessage(TagReplacementEngine.replace(msgCtx.getMessage(), tags, msgCtx.getPlayer(), maxReplacements));
    }

    @Override
    public @NonNull String getId() { return ID; }
}