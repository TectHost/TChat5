package tect.host.tpl.module.impl.chat.chatplaceholders.custom;

import org.jspecify.annotations.NonNull;

import java.util.List;

public record CustomChatTagConfig(String trigger, String permission, String labelTemplate, List<String> actions) {

    public CustomChatTagConfig(@NonNull String trigger, @NonNull String permission, @NonNull String labelTemplate, @NonNull List<String> actions) {
        this.trigger = trigger;
        this.permission = permission;
        this.labelTemplate = labelTemplate;
        this.actions = List.copyOf(actions);
    }
}