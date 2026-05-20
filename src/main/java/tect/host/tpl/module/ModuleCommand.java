package tect.host.tpl.module;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;

public interface ModuleCommand extends BasicCommand {

    @NonNull String getName();

    default @NonNull List<String> getAliases() {
        return List.of();
    }

    @Override
    default @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, String @NonNull [] args) {
        return List.of();
    }
}