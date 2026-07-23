package tect.host.tpl.util.logging;

import org.bukkit.Bukkit;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.TChat;
import tect.host.tpl.util.Utils;

import java.util.logging.Logger;

public final class StartupLogger {

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String PURPLE = "\u001B[95m";
    private static final String AQUA = "\u001B[96m";
    private static final String WHITE = "\u001B[97m";
    private static final String GRAY = "\u001B[90m";
    private static final String GREEN = "\u001B[92m";
    private static final String YELLOW = "\u001B[93m";
    private static final String RED = "\u001B[91m";

    private static final String SEP = GRAY + "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET;
    private static final int DOT_W = 16;

    private enum Status { SUCCESS, WARNING, ERROR }

    private final Logger logger;
    private final String version;
    private final String serverBrand;
    private final String javaVersion;

    public StartupLogger(@NonNull TChat plugin) {
        this.logger = plugin.getLogger();
        this.version = Utils.getPluginVersion(plugin);
        this.serverBrand = resolveServerBrand();
        this.javaVersion = System.getProperty("java.version", "?");
    }

    public record Stats(int modulesLoaded, int modulesSkipped, int modulesFailed, int menusEnabled, int menusDisabled, int menusSkipped, int commandsRegistered, int commandsSkipped, int hooksActive, int hooksMissing, long elapsedMillis, boolean debugEnabled, double dependencyResolutionMs, double modulesLoadMs, @Nullable String slowestModuleId, double slowestModuleMs) {}

    public void print(@NonNull Stats s, @NonNull StartupMode mode) {
        String modeLabel = PURPLE + "TChat" + RESET;

        raw("");
        raw(SEP);
        raw("  " + BOLD + modeLabel + RESET + WHITE + "  v" + version + RESET);
        raw(SEP);
        raw("");

        if (mode == StartupMode.STARTUP) {
            section("Runtime");
            entry(Status.SUCCESS, "Server", serverBrand);
            entry(Status.SUCCESS, "Java", javaVersion);
            raw("");
            raw(SEP);
            raw("");
        }

        section("Modules");
        entry(Status.SUCCESS, "Loaded", s.modulesLoaded() + " modules");
        entry(s.modulesSkipped() == 0 ? Status.SUCCESS : Status.WARNING, "Skipped", s.modulesSkipped() + " modules");
        if (s.modulesFailed() > 0) entry(Status.ERROR, "Failed", s.modulesFailed() + " modules");
        raw("");

        raw(SEP);
        raw("");

        section("Menus");
        entry(Status.SUCCESS, "Enabled", s.menusEnabled() + " menus");
        entry(s.menusDisabled() == 0 ? Status.SUCCESS : Status.WARNING, "Disabled", s.menusDisabled() + " menus");
        entry(s.menusSkipped() == 0 ? Status.SUCCESS : Status.WARNING, "Skipped", s.menusSkipped() + " menus");
        raw("");

        raw(SEP);
        raw("");

        section("Commands");
        entry(Status.SUCCESS, "Registered", s.commandsRegistered() + " commands");
        entry(s.commandsSkipped() == 0 ? Status.SUCCESS : Status.WARNING, "Skipped", s.commandsSkipped() + " commands");
        raw("");

        raw(SEP);
        raw("");

        section("Hooks");
        entry(Status.SUCCESS, "Active", s.hooksActive() + " hooks");
        entry(s.hooksMissing() == 0 ? Status.SUCCESS : Status.WARNING, "Missing", s.hooksMissing() + " optionals");
        raw("");

        raw(SEP);
        raw("");

        section("Status");
        entry(Status.SUCCESS, "Startup", mode == StartupMode.RELOAD ? "reloaded" : "completed");
        info("Time", s.elapsedMillis() + "ms");

        if (s.debugEnabled()) {
            info("Dependencies", "%.2fms".formatted(s.dependencyResolutionMs()));
            info("Modules load", "%.2fms".formatted(s.modulesLoadMs()));
            if (s.slowestModuleId() != null) {
                info("Slowest module", "%s (%.2fms)".formatted(s.slowestModuleId(), s.slowestModuleMs()));
            }
        }

        raw("");

        raw(SEP);
        raw("");
    }

    private void section(@NonNull String title) {
        raw(AQUA + "  »  " + BOLD + title + RESET);
    }

    private void entry(@NonNull Status status, @NonNull String key, @NonNull String value) {
        String badge = switch (status) {
            case SUCCESS -> GREEN + "[+]" + RESET;
            case WARNING -> YELLOW + "[!]" + RESET;
            case ERROR -> RED + "[-]" + RESET;
        };
        raw("  " + badge + " " + dotted(key) + WHITE + " " + value + RESET);
    }

    private void info(@NonNull String key, @NonNull String value) {
        raw("  " + YELLOW + "[i]" + RESET + " " + dotted(key) + GRAY + " " + value + RESET);
    }

    private void raw(@NonNull String line) {
        Utils.log(logger, "INFO", line);
    }

    private static @NonNull String dotted(@NonNull String key) {
        int dots = Math.max(2, DOT_W - key.length());
        return WHITE + key + " " + GRAY + ".".repeat(dots) + RESET;
    }

    private static @NonNull String resolveServerBrand() {
        String name = Bukkit.getName();
        String mcVersion = Bukkit.getMinecraftVersion();
        return name + " " + mcVersion;
    }
}