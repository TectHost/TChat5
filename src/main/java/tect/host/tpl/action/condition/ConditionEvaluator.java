package tect.host.tpl.action.condition;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;

import java.util.List;

public final class ConditionEvaluator {

    private ConditionEvaluator() {}

    private static final List<String> OPERATORS = List.of(">=", "<=", "==", "!=", ">", "<");

    public static boolean evaluate(@NonNull Player player, @NonNull PlaceholderApiHook papi, @NonNull String expression) {
        String resolved = papi.apply(player, expression.replace("{player}", player.getName()));

        for (String op : OPERATORS) {
            int idx = resolved.indexOf(op);
            if (idx == -1) continue;
            String left = unquote(resolved.substring(0, idx).strip());
            String right = unquote(resolved.substring(idx + op.length()).strip());
            return compare(left, right, op);
        }

        // no operator = boolean
        String trimmed = resolved.strip();
        return trimmed.equalsIgnoreCase("true") || trimmed.equals("1");
    }

    private static boolean compare(@NonNull String left, @NonNull String right, @NonNull String op) {
        Double l = tryParse(left), r = tryParse(right);
        if (l != null && r != null) {
            return switch (op) {
                case "==" -> l.doubleValue() == r.doubleValue();
                case "!=" -> l.doubleValue() != r.doubleValue();
                case ">=" -> l >= r;
                case "<=" -> l <= r;
                case ">" -> l > r;
                case "<" -> l < r;
                default -> false;
            };
        }
        return switch (op) {
            case "==" -> left.equalsIgnoreCase(right);
            case "!=" -> !left.equalsIgnoreCase(right);
            default -> false;
        };
    }

    private static @Nullable Double tryParse(@NonNull String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }

    private static @NonNull String unquote(@NonNull String s) {
        if (s.length() >= 2 && (s.charAt(0) == '\'' || s.charAt(0) == '"') && s.charAt(s.length() - 1) == s.charAt(0)) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}