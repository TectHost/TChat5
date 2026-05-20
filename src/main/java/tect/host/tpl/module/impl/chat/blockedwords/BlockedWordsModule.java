package tect.host.tpl.module.impl.chat.blockedwords;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.util.ColorUtil;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.util.Utils;

import java.util.*;

public final class BlockedWordsModule implements ChatModule {

    private static final String ID = "blocked-words";
    private static final String BYPASS_PERM = "tchat.admin.bypass.blockedwords";

    private record LoadedState(BlockedWordsConfig config, SequencedSet<String> words, Map<String, String> cache, List<Component> blockMessageLines) {
        @Contract("_ -> new")
        static @NonNull LoadedState from(@NonNull BlockedWordsConfig config) {
            SequencedSet<String> words = new LinkedHashSet<>(config.getBlockedWords());
            return new LoadedState(config, words, buildCache(words), buildMessageComponents(config));
        }

        @Contract("_, _ -> new")
        static @NonNull LoadedState withWords(@NonNull BlockedWordsConfig config, @NonNull SequencedSet<String> words) {
            return new LoadedState(config, new LinkedHashSet<>(words), buildCache(words), buildMessageComponents(config));
        }

        private static @NonNull @UnmodifiableView Map<String, String> buildCache(@NonNull Set<String> words) {
            Map<String, String> map = new LinkedHashMap<>(words.size());
            for (String w : words) {
                String stripped = BlockedWordsMatcher.stripWord(w);
                if (!stripped.isEmpty()) map.put(w, stripped);
            }
            return Collections.unmodifiableMap(map);
        }

        private static @NonNull List<Component> buildMessageComponents(@NonNull BlockedWordsConfig config) {
            List<String> lines = config.getBlockMessage();
            if (lines.isEmpty()) return List.of();
            return lines.stream().map(ColorUtil::legacyToMini).map(ColorUtil::deserialize).toList();
        }
    }

    private final ModuleContext moduleContext;
    private ConfigFile configFile;

    private volatile LoadedState state;

    public BlockedWordsModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        configFile = moduleContext.createConfigFile("blockedwords.yml", "modules");
        configFile.setMigrator(BlockedWordsMigrations.create(moduleContext.getLogger()));
        configFile.register();
        load();
    }

    @Override
    public void onReload() {
        configFile.reload();
        load();
    }

    private void load() {
        state = LoadedState.from(new BlockedWordsConfig(configFile));
    }

    @Override
    public void process(@NonNull MessageContext ctx) {
        LoadedState snap = state;
        if (snap == null || snap.cache().isEmpty()) return;
        if (Utils.hasPerms(ctx.getPlayer(), BYPASS_PERM)) return;

        switch (snap.config().getAction()) {
            case BLOCK -> {
                BlockedWordsMatcher.MatchResult result = BlockedWordsMatcher.check(ctx.getRawMessage(), snap.cache());
                if (!result.matched()) return;

                ctx.setCancelled(true);

                List<Component> components = snap.blockMessageLines();
                if (!components.isEmpty()) {
                    Player player = ctx.getPlayer();
                    for (Component component : components) {
                        player.sendMessage(component);
                    }
                }
            }
            case CENSOR_ALL -> {
                String result = BlockedWordsMatcher.censorAll(ctx.getRawMessage(), snap.cache(), snap.config().getCensorChar());
                if (result != null) ctx.setRawOverride(result);
            }
            case CENSOR -> {
                String result = BlockedWordsMatcher.censor(ctx.getRawMessage(), snap.cache(), snap.config().getCensorChar());
                if (result != null) ctx.setRawOverride(result);
            }
        }
    }

    /**
     * Returns false if the word was already present
     */
    public synchronized boolean addWord(@NonNull String word) {
        word = word.strip();
        LoadedState current = state;
        SequencedSet<String> updated = new LinkedHashSet<>(current.words());
        if (!updated.add(word)) return false;
        state = LoadedState.withWords(current.config(), updated);
        persistWords(updated);
        return true;
    }

    /**
     * Returns false if the word was not present
     */
    public synchronized boolean removeWord(@NonNull String word) {
        word = word.strip();
        LoadedState current = state;
        SequencedSet<String> updated = new LinkedHashSet<>(current.words());
        if (!updated.remove(word)) return false;
        state = LoadedState.withWords(current.config(), updated);
        persistWords(updated);
        return true;
    }

    private void persistWords(@NonNull SequencedSet<String> words) {
        configFile.get().set("blocked-words", words.stream().toList());
        configFile.save();
    }

    public @NonNull @UnmodifiableView SequencedSet<String> getWords() {
        return Collections.unmodifiableSequencedSet(state.words());
    }

    @Override
    public @NonNull String getId() { return ID; }
}