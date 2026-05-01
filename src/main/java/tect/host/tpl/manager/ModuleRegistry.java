package tect.host.tpl.manager;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.module.ModulePhase;
import tect.host.tpl.module.impl.chat.colorchat.ColorChatModule;
import tect.host.tpl.module.impl.chat.group.GroupModule;
import tect.host.tpl.module.impl.chat.format.FormatModule;
import tect.host.tpl.module.impl.join.notify.UpdateNotifyModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleRegistry {

    private ModuleRegistry() {}

    /*
     * Registration order does not affect execution order
     * Execution order is determined by phase and priority defined in each descriptor
     * Dependency resolution (Kahn's algorithm) uses registration order only as a tiebreaker
     * when multiple modules become available simultaneously, this has no observable effect
     */

    /*
     * In togglePath, I just need to specify the module according to config.yml
     * I don't need to include “modules.<module>” since that is added by the ConfigManager
     */

    /*
     * Sample code for adding a module that does not modify the chat, only commands or messages
     * (remember: don't include .phase, unless the module modifies the chat)
     *
     * ModuleDescriptor.builder("announcements", "announcements", AnnouncementsModule::new)
     *   .build()
     */

    /**
     * The reason for putting multiple categories into an array is that
     * TChat will eventually have more than 50 modules
     * otherwise, I would create a list and just throw everything into it without any order
     */
    @Contract(" -> new")
    public static @NonNull @UnmodifiableView List<ModuleDescriptor> createDefaultRegistry() {
        List<ModuleDescriptor> all = new ArrayList<>();

        all.addAll(chatModules());
        all.addAll(joinModules());

        return Collections.unmodifiableList(all);
    }

    @Contract(" -> new")
    private static @NonNull @Unmodifiable List<ModuleDescriptor> chatModules() {
        return List.of(
                ModuleDescriptor.builder("colorchat", "colorchat", ColorChatModule::new)
                        .phase(ModulePhase.PRE_PROCESS).priority(8).build(),
                ModuleDescriptor.builder("group", "group", GroupModule::new)
                        .phase(ModulePhase.FORMAT).priority(9).build(),
                ModuleDescriptor.builder("format", "format", FormatModule::new)
                        .phase(ModulePhase.FORMAT).priority(10).build()
        );
    }

    @Contract(" -> new")
    private static @NonNull @Unmodifiable List<ModuleDescriptor> joinModules() {
        return List.of(
                // In the future, there will be a "Join" module,
                // and it should take precedence over the "update-notify" module
                //
                // ModuleDescriptor.builder("join", "join", JoinModule::new)
                //     .priority(9)
                //     .build(),
                ModuleDescriptor.builder("update-notify", "update-notify", UpdateNotifyModule::new)
                        .priority(10)
                        .build()
        );
    }
}