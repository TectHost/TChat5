package tect.host.tpl.module.impl.command.customcommands;

import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.CommandModule;
import tect.host.tpl.context.CommandContext;
import tect.host.tpl.util.Utils;

import java.io.File;
import java.util.*;

public final class CustomCommandsModule implements CommandModule {

    private static final String ID = "custom-commands";

    private static final String COMMANDS_SUBFOLDER = "modules/customcommands";

    private static final List<String> DEFAULT_FILES = List.of(
            "announcement.yml",
            "broadcast.yml",
            "chatclear.yml",
            "conditionals.yml",
            "discord.yml",
            "facebook.yml",
            "heal.yml",
            "help.yml",
            "instagram.yml",
            "me.yml",
            "plugins.yml",
            "print.yml",
            "showcoords.yml",
            "tiktok.yml",
            "twitter.yml",
            "warning.yml",
            "website.yml",
            "youtube.yml"
    );

    private final ModuleContext moduleContext;

    private File commandsFolder;

    private volatile Map<String, CustomCommandExecutor> executors = Map.of();
    private Set<String> registeredNames = new HashSet<>();

    public CustomCommandsModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        commandsFolder = new File(moduleContext.getPluginDataFolder(), COMMANDS_SUBFOLDER);

        if (!commandsFolder.isDirectory()) {
            copyDefaults();
        }

        executors = buildExecutors();
        registeredNames = collectAllNames(executors);
    }

    @Override
    public void onReload() {
        Map<String, CustomCommandExecutor> fresh = buildExecutors();

        Set<String> freshNames = collectAllNames(fresh);

        Set<String> added = new HashSet<>(freshNames);
        added.removeAll(registeredNames);
        if (!added.isEmpty()) {
            Utils.log(moduleContext.getLogger(), "WARNING", "[CustomCommands] New commands require a restart to activate: " + added);
        }

        Set<String> removed = new HashSet<>(registeredNames);
        removed.removeAll(freshNames);
        if (!removed.isEmpty()) {
            Utils.log(moduleContext.getLogger(), "WARNING", "[CustomCommands] Removed commands still registered until restart: " + removed);
        }

        executors = fresh;
        moduleContext.getDebugLogger().info("[CustomCommands] Reloaded %d custom command(s).".formatted(fresh.size()));
    }

    @Override
    public void onDisable() {
        executors = Map.of();
    }

    @Override
    public void process(@NonNull CommandContext ctx) {}

    @Override
    public @NonNull String getId() { return ID; }

    public @NonNull @UnmodifiableView Collection<CustomCommandExecutor> getExecutors() {
        return Collections.unmodifiableCollection(executors.values());
    }

    private void copyDefaults() {
        for (String fileName : DEFAULT_FILES) {
            ConfigFile configFile = moduleContext.createConfigFile(fileName, COMMANDS_SUBFOLDER);
            configFile.register();
        }
    }

    private @NonNull Map<String, CustomCommandExecutor> buildExecutors() {
        if (!commandsFolder.isDirectory()) {
            moduleContext.getDebugLogger().warn("[CustomCommands] Commands folder missing: " + commandsFolder.getPath());
            return Map.of();
        }

        String[] fileNames = commandsFolder.list((dir, name) -> name.endsWith(".yml"));

        if (fileNames == null || fileNames.length == 0) {
            moduleContext.getDebugLogger().info("[CustomCommands] No command files found in " + commandsFolder.getPath());
            return Map.of();
        }

        Arrays.sort(fileNames);

        Map<String, CustomCommandExecutor> map = new LinkedHashMap<>(fileNames.length * 2);

        for (String fileName : fileNames) {
            String commandName = fileName.replace(".yml", "");
            try {
                ConfigFile configFile = moduleContext.createConfigFile(fileName, COMMANDS_SUBFOLDER);
                configFile.setMigrator(CustomCommandsMigrations.create(moduleContext.getLogger()));
                configFile.register();

                CustomCommandDefinition def = CustomCommandConfig.parse(commandName, configFile);

                if (map.containsKey(def.name())) {
                    moduleContext.getDebugLogger().warn("[CustomCommands] Duplicate name '%s' (file '%s'), skipping.".formatted(def.name(), fileName));
                    continue;
                }

                map.put(def.name(), new CustomCommandExecutor(def, moduleContext.getActionExecutor()));

                moduleContext.getDebugLogger().info("[CustomCommands] Loaded /%s%s".formatted(def.name(), def.aliases().isEmpty() ? "" : " (aliases: " + String.join(", ", def.aliases()) + ")"));
            } catch (Exception e) {
                moduleContext.getDebugLogger().severe("[CustomCommands] Failed to load '%s': %s".formatted(fileName, e.getMessage()));
            }
        }

        moduleContext.getDebugLogger().info("[CustomCommands] Loaded %d custom command(s).".formatted(map.size()));
        return Collections.unmodifiableMap(map);
    }

    private static @NonNull Set<String> collectAllNames(@NonNull Map<String, CustomCommandExecutor> map) {
        Set<String> names = new HashSet<>();
        for (CustomCommandExecutor ex : map.values()) {
            names.add(ex.getName());
            names.addAll(ex.getAliases());
        }
        return names;
    }
}