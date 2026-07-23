package tect.host.tpl.action.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

sealed public interface ActionNode permits ActionNode.Single, ActionNode.Conditional, ActionNode.Loop {

    record Single(@NonNull String type, @NonNull String arg) implements ActionNode {}

    record Conditional(@NonNull List<Branch> branches) implements ActionNode {}

    record Branch(@Nullable String condition, @NonNull List<ActionNode> body) {}

    record Loop(@NonNull String countExpr, @NonNull List<ActionNode> body) implements ActionNode {}
}