package tect.host.tpl.action;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.action.model.ActionNode;

import java.util.List;

public record CompiledActions(@NonNull List<ActionNode> nodes) {
    public static final CompiledActions EMPTY = new CompiledActions(List.of());
}