package tect.host.tpl.module.impl.broadcast.autobroadcast;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.SchedulerAccess;
import tect.host.tpl.module.impl.chat.channel.ChannelModule;
import tect.host.tpl.module.impl.chat.channel.ChannelService;
import tect.host.tpl.module.type.BroadcastModule;
import tect.host.tpl.util.CenterUtil;
import tect.host.tpl.util.ColorUtil;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class AutoBroadcastModule implements BroadcastModule {

    private static final String ID = "auto-broadcast";

    private record LoadedState(@NonNull AutoBroadcastConfig config) {}

    private final ModuleContext moduleContext;
    private ConfigFile configFile;

    private volatile LoadedState state;
    private final AtomicInteger index = new AtomicInteger(0);
    private SchedulerAccess.@Nullable Cancellable timerHandle;

    public AutoBroadcastModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        configFile = moduleContext.createConfigFile("autobroadcast.yml", "modules");
        configFile.setMigrator(AutoBroadcastMigrations.create(moduleContext.getLogger()));
        configFile.register();
        load();
        scheduleTimer();
    }

    @Override
    public void onReload() {
        cancelTimer();
        configFile.reload();
        load();
        index.set(0);
        scheduleTimer();
    }

    @Override
    public void onDisable() {
        cancelTimer();
    }

    private void load() {
        state = new LoadedState(new AutoBroadcastConfig(configFile));
    }

    private void scheduleTimer() {
        LoadedState snap = state;
        if (snap == null || snap.config().getEntries().isEmpty()) return;

        long periodTicks = snap.config().getIntervalSeconds() * 20L;
        timerHandle = moduleContext.getScheduler().runTimer(this::broadcast, periodTicks, periodTicks);
    }

    private void cancelTimer() {
        if (timerHandle != null) {
            timerHandle.cancel();
            timerHandle = null;
        }
    }

    private void broadcast() {
        LoadedState snap = state;
        if (snap == null) return;

        List<AutoBroadcastEntry> entries = snap.config().getEntries();
        if (entries.isEmpty()) return;

        int i = index.getAndUpdate(cur -> (cur + 1) % entries.size());
        AutoBroadcastEntry entry = entries.get(i);

        Collection<? extends Player> online = moduleContext.getOnlinePlayers();
        List<Player> recipients = resolveRecipients(entry, online);

        for (Player player : recipients) {
            if (entry.permission().isPresent() && !player.hasPermission(entry.permission().get())) continue;
            sendLines(player, entry.rawMessages());
        }
    }

    private @NonNull List<Player> resolveRecipients(@NonNull AutoBroadcastEntry entry, @NonNull Collection<? extends Player> online) {
        if (entry.channel().isEmpty()) {
            return List.copyOf(online);
        }

        String channelId = entry.channel().get();
        ChannelModule channelModule = moduleContext.getModuleManager().getModule(ChannelModule.ID, ChannelModule.class);

        if (channelModule == null || channelModule.getChannelService() == null) {
            moduleContext.getLogger().warning("AutoBroadcast entry '%s' targets channel '%s' but ChannelModule is not active — broadcasting to all.".formatted(entry.id(), channelId));
            return List.copyOf(online);
        }

        ChannelService service = channelModule.getChannelService();
        var channelEntry = service.getChannel(channelId);

        if (channelEntry == null) {
            moduleContext.getLogger().warning("AutoBroadcast entry '%s' targets unknown channel '%s' — broadcasting to all.".formatted(entry.id(), channelId));
            return List.copyOf(online);
        }

        return service.resolveRecipients(channelEntry, online);
    }

    private void sendLines(@NonNull Player player, @NonNull List<String> rawLines) {
        var papiHook = moduleContext.getPlaceholderApiHook();
        for (String raw : rawLines) {
            String resolved = papiHook.apply(player, raw);
            String centered = CenterUtil.center(resolved);
            Component component = ColorUtil.deserialize(ColorUtil.legacyToMiniSafe(centered));
            player.sendMessage(component);
        }
    }

    @Override
    public @NonNull String getId() { return ID; }
}