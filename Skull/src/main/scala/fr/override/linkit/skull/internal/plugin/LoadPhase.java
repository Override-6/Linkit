package fr.override.linkit.skull.internal.plugin;

import fr.override.linkit.skull.internal.plugin.fragment.PluginFragment;

public enum LoadPhase {

    /**
     * Defines the loader to be in "loading" phase.
     * "loading" phase is set when all extensions are enabling ({@link Plugin#onLoad()})
     */
    LOADING,
    /**
     * Defines the loader to be in "Enabling" phase.
     * "enable" phase is set when extensions and fragments are enabling ({@link Plugin#onEnable()} & {@link PluginFragment#start()})
     */
    ENABLING,
    /**
     * Defines the loader to be in "Disabling" phase.
     * "disable" phase is set when extensions and fragments are disabling ({@link Plugin#onDisable()} & {@link PluginFragment#destroy()})
     */
    DISABLING,
    /**
     * Defines the loader to be in "Inactive" phase.
     * The Inactive phase is set when no extensions and fragments are running.
     * the ExtensionLoader is set as INACTIVE until no extensions are loaded
     */
    INACTIVE,
    /**
     * Defines the loader to be in "Active" phase.
     * "Active" phase is set when extensions and fragments are all running
     */
    ACTIVE,
    /**
     * Defines the loader to be in "Closed" phase.
     * Once closed, the loader can't perform any operation (would throw {@link IllegalStateException}).
     */
    CLOSE

}
