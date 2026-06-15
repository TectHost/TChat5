package tect.host.tpl.module.impl.command.invsee;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.context.JoinContext;
import tect.host.tpl.context.QuitContext;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.JoinModule;
import tect.host.tpl.module.type.QuitModule;

import java.util.ArrayList;
import java.util.List;

public final class InvSeeModule implements JoinModule, QuitModule {

    private static final String ID = "invsee";

    private final ModuleContext ctx;
    private ConfigFile configFile;
    private volatile InvSeeConfig config;

    public InvSeeModule(@NonNull ModuleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onEnable() {
        configFile = ctx.createConfigFile("invsee.yml", "modules");
        configFile.register();
        config = new InvSeeConfig(configFile);
    }

    @Override
    public void onReload() {
        configFile.reload();
        config = new InvSeeConfig(configFile);
    }

    @Override
    public void onJoin(@NonNull JoinContext ctx) {}

    @Override
    public void onQuit(@NonNull QuitContext quitCtx) {
        var quitting = quitCtx.getPlayer();

        List<HumanEntity> toClose = new ArrayList<>();

        for (var viewer : quitting.getServer().getOnlinePlayers()) {
            Inventory open = viewer.getOpenInventory().getTopInventory();
            InventoryHolder holder = open.getHolder();
            if (holder instanceof InvSeeHolder invHolder && invHolder.getTarget().equals(quitting)) {
                toClose.add(viewer);
            }
        }

        toClose.forEach(HumanEntity::closeInventory);
    }

    public @NonNull InvSeeConfig getConfig() { return config; }

    @Override
    public @NonNull String getId() { return ID; }
}