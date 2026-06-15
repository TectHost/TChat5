package tect.host.tpl.module.impl.chat.chatplaceholders.custom;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.action.ActionExecutor;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;
import tect.host.tpl.module.impl.chat.chatplaceholders.ChatTag;
import tect.host.tpl.util.ColorUtil;

public final class CustomChatTag implements ChatTag {

    private final CustomChatTagConfig config;
    private final ActionExecutor actionExecutor;
    private final PlaceholderApiHook placeholderApiHook;

    public CustomChatTag(@NonNull CustomChatTagConfig config, @NonNull ActionExecutor actionExecutor, @NonNull PlaceholderApiHook placeholderApiHook) {
        this.config = config;
        this.actionExecutor = actionExecutor;
        this.placeholderApiHook = placeholderApiHook;
    }

    @Override
    public @NonNull String getTrigger() { return config.trigger(); }

    @Override
    public @NonNull String getPermission() { return config.permission(); }

    @Override
    public @NonNull Component resolve(@NonNull Player player) {
        if (!config.actions().isEmpty()) {
            actionExecutor.execute(player, config.actions());
        }
        return ColorUtil.translate(placeholderApiHook, player, config.labelTemplate());
    }
}