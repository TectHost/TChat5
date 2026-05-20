package tect.host.tpl.module.impl.chat.blockedwords;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BlockedWordsConfig {

    private final Action action;
    private final char censorChar;
    private final Set<String> blockedWords;
    private final List<String> blockMessage;

    public BlockedWordsConfig(@NonNull ConfigFile configFile) {
        this.action = Action.fromString(configFile.get().getString("action", "CENSOR"));

        this.censorChar = configFile.get().getString("censor-char", "*").charAt(0);

        List<String> raw = configFile.get().getStringList("blocked-words");
        Set<String> words = new LinkedHashSet<>(raw.size());
        for (String w : raw) {
            if (w != null && !w.isBlank()) words.add(w.strip());
        }
        this.blockedWords = Collections.unmodifiableSet(words);

        List<String> rawMsg = configFile.get().getStringList("block-message");
        this.blockMessage = rawMsg.stream().filter(l -> l != null && !l.isBlank()).toList();
    }

    public @NonNull Action getAction() { return action; }
    public char getCensorChar() { return censorChar; }
    public @NonNull Set<String> getBlockedWords() { return blockedWords; }
    public @NonNull List<String> getBlockMessage() { return blockMessage; }
}