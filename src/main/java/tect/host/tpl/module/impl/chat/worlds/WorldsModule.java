package tect.host.tpl.module.impl.chat.worlds;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.ChatModule;

import java.util.Set;

public final class WorldsModule implements ChatModule {

    private static final String ID = "worlds";

    private static final String BYPASS_CHAT_PERM   = "tchat.admin.bypass.worlds.chat";

    private final ModuleContext moduleContext;
    private ConfigFile configFile;

    private volatile WorldsConfig config;

    public WorldsModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        configFile = moduleContext.createConfigFile("worlds.yml", "modules");
        configFile.setMigrator(WorldsMigrations.create(moduleContext.getLogger()));
        configFile.register();
        load();
    }

    @Override
    public void onReload() {
        configFile.reload();
        load();
    }

    private void load() {
        config = new WorldsConfig(configFile);
    }

    @Override
    public void process(@NonNull MessageContext ctx) {
        WorldsConfig snap = config;
        if (snap == null) return;

        Player sender = ctx.getPlayer();
        WorldConfig worldCfg = snap.forWorld(sender.getWorld().getName());

        // chat-enabled
        if (!worldCfg.chatEnabled() && !sender.hasPermission(BYPASS_CHAT_PERM)) {
            moduleContext.getMessagesManager().sendMessage(sender, "worlds-chat-disabled");
            ctx.setCancelled(true);
            return;
        }

        // radius check
        if (!worldCfg.hasRadiusRestriction()) return;

        Set<? extends Player> recipients = ctx.getRecipients();
        if (recipients == null) return;

        String raw = ctx.getRawMessage();
        if (!raw.isEmpty() && raw.charAt(0) == worldCfg.bypassChar()) {
            ctx.setRawOverride(raw.length() > 1 ? raw.substring(1) : raw);
            return;
        }

        Location origin = sender.getLocation();
        double radiusSq = (double) worldCfg.radius() * worldCfg.radius();

        recipients.removeIf(recipient -> recipient != sender && !isWithinRadius(recipient.getLocation(), origin, radiusSq));
    }

    /**
     * Inline distance-squared check, avoids sqrt, avoids Location.distance() object churn
     * Only compares XZ (no vertical restriction)
     */
    private static boolean isWithinRadius(@NonNull Location loc, @NonNull Location origin, double radiusSq) {
        if (!loc.getWorld().equals(origin.getWorld())) return false;
        double dx = loc.getX() - origin.getX();
        double dz = loc.getZ() - origin.getZ();
        return dx * dx + dz * dz <= radiusSq;
    }

    @Override
    public @NonNull String getId() { return ID; }
}