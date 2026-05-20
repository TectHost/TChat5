package tect.host.tpl.module.impl.command.nick;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.module.type.JoinModule;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.QuitModule;
import tect.host.tpl.context.JoinContext;
import tect.host.tpl.context.QuitContext;

import java.util.Optional;
import java.util.UUID;

public final class NickModule implements JoinModule, QuitModule {

    private static final String ID = "nick";

    private final ModuleContext ctx;
    private NickRepository nickRepo;

    public NickModule(@NonNull ModuleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onEnable() {
        nickRepo = new NickRepository(ctx.getDataManager(), ctx.getScheduler().asExecutor(), ctx.getLogger());

        ctx.getDataManager().initializeRepository(nickRepo);

        ctx.getOnlinePlayers().forEach(p -> nickRepo.preload(p.getUniqueId()));
    }

    @Override
    public void onDisable() {
        if (nickRepo != null) nickRepo.invalidateAll();
    }

    @Override
    public void onReload() {
        if (nickRepo != null) {
            nickRepo.invalidateAll();
            ctx.getOnlinePlayers().forEach(p -> nickRepo.preload(p.getUniqueId()));
        }
    }

    @Override
    public void onJoin(@NonNull JoinContext ctx) {
        nickRepo.preload(ctx.getPlayer().getUniqueId());
    }

    @Override
    public void onQuit(@NonNull QuitContext ctx) {
        nickRepo.evict(ctx.getPlayer().getUniqueId());
    }

    public @NonNull Optional<String> getNick(@NonNull UUID uuid) {
        return nickRepo.getNickCached(uuid);
    }

    public void setNick(@NonNull Player target, @NonNull String nick) {
        nickRepo.setNick(target.getUniqueId(), nick)
                .thenRun(() -> ctx.getScheduler().runSync(() -> target.displayName(Component.text(nick))))
                .exceptionally(ex -> {
                    ctx.getLogger().warning("setNick failed for %s, displayName not updated: %s".formatted(target.getName(), ex.getCause().getMessage()));
                    return null;
                });
    }

    public void removeNick(@NonNull Player target) {
        nickRepo.removeNick(target.getUniqueId())
                .thenRun(() -> ctx.getScheduler().runSync(() -> target.displayName(Component.text(target.getName()))))
                .exceptionally(ex -> {
                    ctx.getLogger().warning("removeNick failed for %s, displayName not updated: %s".formatted(target.getName(), ex.getCause().getMessage()));
                    return null;
                });
    }

    @Override
    public @NonNull String getId() { return ID; }
}