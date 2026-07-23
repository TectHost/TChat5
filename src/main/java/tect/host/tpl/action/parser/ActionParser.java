package tect.host.tpl.action.parser;

import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.action.model.ActionNode;
import tect.host.tpl.util.Utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Logger;

public final class ActionParser {

    private ActionParser() {}

    private record ParsedLine(@NonNull String type, @NonNull String arg) {}

    private abstract static class Frame {
        final List<ActionNode> parentBody;

        Frame(List<ActionNode> parentBody) {
            this.parentBody = parentBody;
        }

        abstract @NonNull List<ActionNode> body();

        abstract void close();
    }

    private static final class IfFrame extends Frame {
        final List<ActionNode.Branch> branches = new ArrayList<>();
        String condition;
        List<ActionNode> body = new ArrayList<>();

        IfFrame(List<ActionNode> parentBody, String condition) {
            super(parentBody);
            this.condition = condition;
        }

        @Override
        @NonNull List<ActionNode> body() { return body; }

        void newBranch(@Nullable String nextCondition) {
            branches.add(new ActionNode.Branch(condition, List.copyOf(body)));
            condition = nextCondition;
            body = new ArrayList<>();
        }

        @Override
        void close() {
            branches.add(new ActionNode.Branch(condition, List.copyOf(body)));
            parentBody.add(new ActionNode.Conditional(List.copyOf(branches)));
        }
    }

    private static final class ForFrame extends Frame {
        final String countExpr;
        final List<ActionNode> body = new ArrayList<>();

        ForFrame(List<ActionNode> parentBody, String countExpr) {
            super(parentBody);
            this.countExpr = countExpr;
        }

        @Override
        @NonNull List<ActionNode> body() { return body; }

        @Override
        void close() {
            parentBody.add(new ActionNode.Loop(countExpr, List.copyOf(body)));
        }
    }

    public static @NonNull @Unmodifiable List<ActionNode> parse(@NonNull List<String> rawLines, @NonNull Logger log) {
        List<ActionNode> root = new ArrayList<>();
        Deque<Frame> stack = new ArrayDeque<>();
        List<ActionNode> current = root;

        for (String raw : rawLines) {
            if (raw == null || raw.isBlank()) continue;
            ParsedLine parsed = splitTypeArg(raw.strip(), log);
            if (parsed == null) continue;

            switch (parsed.type()) {
                case "IF" -> {
                    Frame frame = new IfFrame(current, parsed.arg());
                    stack.push(frame);
                    current = frame.body();
                }
                case "ELSE IF", "ELSEIF" -> {
                    if (!(stack.peek() instanceof IfFrame ifFrame)) {
                        Utils.log(log, "WARNING", "ActionParser: [ELSE IF] without a preceding [IF]");
                        break;
                    }
                    ifFrame.newBranch(parsed.arg());
                    current = ifFrame.body();
                }
                case "ELSE" -> {
                    if (!(stack.peek() instanceof IfFrame ifFrame)) {
                        Utils.log(log, "WARNING", "ActionParser: [ELSE] without a preceding [IF]");
                        break;
                    }
                    ifFrame.newBranch(null);
                    current = ifFrame.body();
                }
                case "ENDIF" -> {
                    if (!(stack.peek() instanceof IfFrame ifFrame)) {
                        Utils.log(log, "WARNING", "ActionParser: [ENDIF] without a preceding [IF]");
                        break;
                    }
                    stack.pop();
                    ifFrame.close();
                    current = stack.isEmpty() ? root : stack.peek().body();
                }
                case "FOR" -> {
                    if (parsed.arg().isBlank()) {
                        Utils.log(log, "WARNING", "ActionParser: [FOR] missing count expression");
                        break;
                    }
                    Frame frame = new ForFrame(current, parsed.arg());
                    stack.push(frame);
                    current = frame.body();
                }
                case "ENDFOR" -> {
                    if (!(stack.peek() instanceof ForFrame forFrame)) {
                        Utils.log(log, "WARNING", "ActionParser: [ENDFOR] without a preceding [FOR]");
                        break;
                    }
                    stack.pop();
                    forFrame.close();
                    current = stack.isEmpty() ? root : stack.peek().body();
                }
                default -> current.add(new ActionNode.Single(parsed.type(), parsed.arg()));
            }
        }

        if (!stack.isEmpty()) {
            Utils.log(log, "WARNING", "ActionParser: %d block(s) not closed (missing [ENDIF]/[ENDFOR])".formatted(stack.size()));
        }

        return List.copyOf(root);
    }

    private static @Nullable ParsedLine splitTypeArg(@NonNull String line, @NonNull Logger log) {
        if (line.charAt(0) != '[') { warn(line, log); return null; }
        int close = line.indexOf(']');
        if (close < 2) { warn(line, log); return null; }

        String type = line.substring(1, close).toUpperCase();
        String arg = close + 1 < line.length()
                ? line.substring(line.charAt(close + 1) == ' ' ? close + 2 : close + 1)
                : "";

        return new ParsedLine(type, arg);
    }

    private static void warn(@NonNull String line, @NonNull Logger log) {
        Utils.log(log, "WARNING", "ActionParser: malformed line (missing [TYPE]): %s".formatted(line));
    }
}