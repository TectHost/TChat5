package tect.host.tpl.module.impl.chat.chatplaceholders.engine;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.module.impl.chat.chatplaceholders.ChatTag;
import tect.host.tpl.util.ColorUtil;
import tect.host.tpl.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TagReplacementEngine {

    private TagReplacementEngine() {}

    public static @NonNull Component replace(@NonNull Component root, @NonNull Map<String, ChatTag> tags, @NonNull Player player, int maxReplacements) {
        if (tags.isEmpty()) return root;

        String plain = ColorUtil.toPlainText(root).toLowerCase(Locale.ROOT);
        boolean anyMatch = tags.keySet().stream().anyMatch(plain::contains);
        if (!anyMatch) return root;

        int budget = maxReplacements <= 0 ? Integer.MAX_VALUE : maxReplacements;
        int[] remaining = { budget };

        Component current = root;

        for (Map.Entry<String, ChatTag> entry : tags.entrySet()) {
            if (remaining[0] <= 0) break;

            String triggerLower = entry.getKey();
            if (!ColorUtil.toPlainText(current).toLowerCase(Locale.ROOT).contains(triggerLower)) continue;

            ChatTag tag = entry.getValue();

            String perm = tag.getPermission();
            if (!perm.isBlank() && !Utils.hasPerms(player, perm)) continue;

            Component replacement = tag.resolve(player);
            if (replacement == null) continue;

            current = replaceInComponent(current, triggerLower, replacement, remaining);
        }

        return current;
    }

    private static @NonNull Component replaceInComponent(@NonNull Component component, @NonNull String trigger, @NonNull Component replacement, int @NonNull [] remaining) {
        if (remaining[0] <= 0) return component;

        List<Component> oldChildren = component.children();
        List<Component> newChildren = new ArrayList<>(oldChildren.size());
        boolean childChanged = false;

        for (Component child : oldChildren) {
            Component processed = replaceInComponent(child, trigger, replacement, remaining);
            newChildren.add(processed);
            if (processed != child) childChanged = true;
        }

        if (!(component instanceof TextComponent text)) {
            return childChanged ? component.children(newChildren) : component;
        }

        String content = text.content();
        String contentLower = content.toLowerCase(Locale.ROOT);
        int idx = contentLower.indexOf(trigger);

        if (idx == -1) {
            return childChanged ? text.children(newChildren) : text;
        }

        List<Component> parts = new ArrayList<>();
        int cursor = 0;

        while (idx != -1 && remaining[0] > 0) {
            if (idx > cursor) {
                parts.add(text.content(content.substring(cursor, idx)));
            }
            parts.add(replacement);
            remaining[0]--;
            cursor = idx + trigger.length();
            idx = remaining[0] > 0 ? contentLower.indexOf(trigger, cursor) : -1;
        }

        if (cursor < content.length()) {
            parts.add(text.content(content.substring(cursor)));
        }

        Component base = Component.empty();
        for (Component part : parts)  base = base.append(part);
        for (Component child : newChildren) base = base.append(child);

        return base;
    }
}