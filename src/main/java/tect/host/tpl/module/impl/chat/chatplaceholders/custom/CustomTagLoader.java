package tect.host.tpl.module.impl.chat.chatplaceholders.custom;

import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.action.ActionExecutor;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;
import tect.host.tpl.module.impl.chat.chatplaceholders.ChatTag;
import tect.host.tpl.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class CustomTagLoader {

    private CustomTagLoader() {}

    public static @NonNull List<ChatTag> load(@NonNull ConfigFile configFile, @NonNull ActionExecutor actionExecutor, @NonNull PlaceholderApiHook placeholderApiHook, @NonNull Logger logger) {
        List<?> rawList = configFile.get().getList("tags");
        if (rawList == null || rawList.isEmpty()) return List.of();

        List<ChatTag> result = new ArrayList<>(rawList.size());

        for (int i = 0; i < rawList.size(); i++) {
            CustomChatTagConfig cfg = parseEntry(rawList.get(i), i, logger);
            if (cfg == null) continue;
            result.add(new CustomChatTag(cfg, actionExecutor, placeholderApiHook));
        }

        return List.copyOf(result);
    }

    private static @Nullable CustomChatTagConfig parseEntry(@NonNull Object entry, int index, @NonNull Logger logger) {
        String trigger;
        String permission;
        String label;
        List<String> actions = List.of();

        if (entry instanceof ConfigurationSection section) {
            trigger = section.getString("trigger");
            permission = section.getString("permission", "");
            label = section.getString("label");
            actions = section.getStringList("actions");

        } else if (entry instanceof Map<?, ?> map) {
            trigger = str(map, "trigger");
            permission = strOr(map);
            label = str(map, "label");
            Object raw = map.get("actions");
            if (raw instanceof List<?> list) {
                actions = list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
            }
        } else {
            Utils.log(logger, "WARNING", "custom-tags.yml entry #%d is not a valid map, skipping.".formatted(index));
            return null;
        }

        if (trigger == null || trigger.isBlank()) {
            Utils.log(logger, "WARNING", "custom-tags.yml entry #%d is missing 'trigger', skipping.".formatted(index));
            return null;
        }
        if (label == null || label.isBlank()) {
            Utils.log(logger, "WARNING", "custom-tags.yml entry #%d ('%s') is missing 'label', skipping.".formatted(index, trigger));
            return null;
        }

        return new CustomChatTagConfig(trigger, permission, label, actions);
    }

    private static @Nullable String str(@NonNull Map<?, ?> map, @NonNull String key) {
        Object v = map.get(key);
        return v instanceof String s ? s : null;
    }

    private static @NonNull String strOr(@NonNull Map<?, ?> map) {
        Object v = map.get("permission");
        return v instanceof String s ? s : "";
    }
}