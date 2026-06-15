package tect.host.tpl.module.impl.command.invsee;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.module.ModuleMenu;
import tect.host.tpl.module.registry.ModuleManager;

public final class InvSeeMenu implements ModuleMenu {

    private static final String ID = "invsee";

    private final InvSeeModule module;

    public InvSeeMenu(@NonNull ModuleManager moduleManager) {
        this.module = moduleManager.getModule("invsee", InvSeeModule.class);
    }

    @Override
    public @NonNull String getId() { return ID; }

    @Override
    public void open(@NonNull Player player, String @NonNull [] args) {
        if (args.length == 0 || module == null) return;

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) return;

        InvSeeHolder holder = new InvSeeHolder(target, module.getConfig());
        player.openInventory(holder.getInventory());
    }
}