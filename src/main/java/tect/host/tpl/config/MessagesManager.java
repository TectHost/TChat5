package tect.host.tpl.config;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.TChat;
import tect.host.tpl.config.migration.LangMigrations;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;
import tect.host.tpl.util.ColorUtil;

import java.util.Map;

public final class MessagesManager {

    private static final String[] SUPPORTED_LANG_FILES = {"en.yml", "es.yml"};

    private final TChat plugin;
    private final ConfigManager configManager;
    private final PlaceholderApiHook placeholderApiHook;

    private ConfigFile selectedLangFile;
    private FileConfiguration messages;

    private @NonNull Component prefix = Component.empty();

    public MessagesManager(@NonNull TChat plugin, @NonNull ConfigManager configManager, @NonNull PlaceholderApiHook placeholderApiHook) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.placeholderApiHook = placeholderApiHook;
        register();
    }

    private void register() {
        ensureAllLanguageFiles();
        loadSelectedLanguage();
    }

    public void reload() {
        loadSelectedLanguage();
    }

    public @NonNull Component getMessage(@NonNull String path, Player player, @NonNull Map<String, String> placeholders) {
        String raw = messages.getString("messages." + path);
        if (raw == null) {
            plugin.getLogger().warning("Missing message key: messages.%s".formatted(path));
            return Component.text("(missing message)");
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace(entry.getKey(), entry.getValue());
        }
        return ColorUtil.translate(placeholderApiHook, player, raw);
    }

    public void sendMessage(@NonNull CommandSender sender, @NonNull String path) {
        sendMessage(sender, path, Map.of());
    }

    public void sendMessage(@NonNull CommandSender sender, @NonNull String path, @NonNull Map<String, String> placeholders) {
        Player player = sender instanceof Player p ? p : null;
        Component message = getMessage(path, player, placeholders);
        sender.sendMessage(prefix.append(Component.space()).append(message));
    }

    private void ensureAllLanguageFiles() {
        for (String langFileName : SUPPORTED_LANG_FILES) {
            ConfigFile langFile = new ConfigFile(plugin, langFileName, "lang");
            langFile.setMigrator(LangMigrations.create(plugin.getLogger(), langFileName));
            langFile.register();
        }
    }

    private void loadSelectedLanguage() {
        String selectedLang = configManager.getString("chat.lang");

        if (selectedLangFile == null || !selectedLangFile.getFileName().equals(selectedLang)) {
            selectedLangFile = new ConfigFile(plugin, selectedLang, "lang");
        }
        selectedLangFile.reload();

        String rawPrefix = selectedLangFile.get().getString("prefix", "");
        prefix = ColorUtil.translate(placeholderApiHook, null, rawPrefix);

        messages = selectedLangFile.get();
    }
}
