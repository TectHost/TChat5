package tect.host.tpl.data;

import org.jspecify.annotations.NonNull;

import java.util.List;

public interface Repository {
    @NonNull String getModuleId();
    @NonNull List<String> getMigrations();

    default void onInitialized() {}
}