package tect.host.tpl.module.impl.chat.anticap;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;

import java.util.List;

public final class AntiCapConfig {

    private final AntiCapAction antiCapAction;
    private final double percent;
    private final char censorChar;
    private final List<String> actions;

    public AntiCapConfig(@NonNull ConfigFile configFile) {
        this.antiCapAction = AntiCapAction.fromString(configFile.get().getString("action", "ToLowerCase"));
        this.percent = Math.clamp(configFile.get().getDouble("percent", 0.75), 0.0, 1.0);
        this.censorChar = configFile.get().getString("censor-char", "*").charAt(0);

        List<String> rawActions = configFile.get().getStringList("actions");
        this.actions = rawActions.stream().filter(l -> l != null && !l.isBlank()).toList();
    }

    public @NonNull AntiCapAction getAction() { return antiCapAction; }
    public double getPercent() { return percent; }
    public char getCensorChar() { return censorChar; }
    public @NonNull List<String> getActions() { return actions; }
}