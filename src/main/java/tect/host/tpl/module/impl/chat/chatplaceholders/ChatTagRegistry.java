package tect.host.tpl.module.impl.chat.chatplaceholders;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.util.Utils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public final class ChatTagRegistry {

    private final Logger logger;

    private final Map<String, ChatTag> tags = new LinkedHashMap<>();

    public ChatTagRegistry(@NonNull Logger logger) {
        this.logger = logger;
    }

    public void register(@NonNull ChatTag tag) {
        String key = tag.getTrigger().toLowerCase(Locale.ROOT);
        if (tags.containsKey(key)) {
            Utils.log(logger, "WARNING", "ChatPlaceholders: duplicate trigger '%s', second registration ignored.".formatted(tag.getTrigger()));
            return;
        }
        tags.put(key, tag);
    }

    public void registerAll(@NonNull Collection<? extends ChatTag> list) {
        list.forEach(this::register);
    }

    @Contract(pure = true)
    public @NonNull @UnmodifiableView Map<String, ChatTag> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    public void clear() {
        tags.clear();
    }

    public int size() { return tags.size(); }
}