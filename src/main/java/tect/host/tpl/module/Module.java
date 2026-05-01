package tect.host.tpl.module;

public interface Module {

    String getId();

    default void onEnable()  {}
    default void onDisable() {}
    default void onReload()  {}
}