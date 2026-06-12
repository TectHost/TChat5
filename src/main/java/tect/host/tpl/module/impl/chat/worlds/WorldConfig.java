package tect.host.tpl.module.impl.chat.worlds;

import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;

public record WorldConfig(boolean chatEnabled, int radius, char bypassChar) {

    private static final WorldConfig DEFAULT = new WorldConfig(true, 0, '!');

    public static @NonNull WorldConfig defaultConfig() { return DEFAULT; }

    public boolean hasRadiusRestriction() { return radius > 0; }

    static @NonNull WorldConfig parse(@NonNull ConfigurationSection sec) {
        boolean chatEnabled = sec.getBoolean("chat-enabled", true);
        int radius = sec.getInt("chat-radius.radius", 0);
        String bypassStr = sec.getString("chat-radius.bypass.char", "!");
        char bypassChar = !bypassStr.isEmpty() ? bypassStr.charAt(0) : '!';
        return new WorldConfig(chatEnabled, radius, bypassChar);
    }
}