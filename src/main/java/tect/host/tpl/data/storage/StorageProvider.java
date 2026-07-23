package tect.host.tpl.data.storage;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.data.DataMethod;

import javax.sql.DataSource;

public interface StorageProvider {

    @NonNull DataMethod method();

    void prepare();

    @NonNull DataSource buildPool();

    @NonNull String migrationsTableDdl();

    default void shutdown() {}
}