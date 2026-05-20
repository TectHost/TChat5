package tect.host.tpl.module.impl.chat.anticap;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;

import java.util.List;

public final class AntiCapConfig {

    private final Action action;
    private final double percent;
    private final char censorChar;
    private final List<String> message;

    public AntiCapConfig(@NonNull ConfigFile configFile) {
        this.action = Action.fromString(configFile.get().getString("action", "ToLowerCase"));
        this.percent = Math.clamp(configFile.get().getDouble("percent", 0.75), 0.0, 1.0);
        this.censorChar = configFile.get().getString("censor-char", "*").charAt(0);

        List<String> rawMsg = configFile.get().getStringList("message");
        this.message = rawMsg.stream().filter(l -> l != null && !l.isBlank()).toList();
    }

    public @NonNull Action getAction() { return action; }
    public double getPercent() { return percent; }
    public char getCensorChar() { return censorChar; }
    public @NonNull List<String> getMessage() { return message; }
}