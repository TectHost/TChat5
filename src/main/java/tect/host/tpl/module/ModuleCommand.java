package tect.host.tpl.module;

import io.papermc.paper.command.brigadier.BasicCommand;
import org.jspecify.annotations.NonNull;

import java.util.List;

public interface ModuleCommand extends BasicCommand {

    @NonNull String getName();

    default @NonNull List<String> getAliases() {
        return List.of();
    }
}