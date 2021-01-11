package fr.override.linkit.api.extension;

import fr.override.linkit.api.extension.fragment.ExtensionFragment;

public enum LoadPhase {

    /**
     * Defines the loader to be in "loading" phase.
     * "loading" phase is set when all extensions are enabling ({@link RelayExtension#onLoad()})
     * */
    LOAD,
    /**
     * Defines the loader to be in "Enabling" phase.
     * "enable" phase is set when extensions and fragments are enabling ({@link RelayExtension#onEnable()} & {@link ExtensionFragment#start()})
     * */
    ENABLE,
    /**
     * Defines the loader to be in "Disabling" phase.
     * "disable" phase is set when extensions and fragments are disabling ({@link RelayExtension#onDisable()} & {@link ExtensionFragment#destroy()})
     * */
    DISABLE,
    /**
     * Defines the loader to be in "Inactive" phase.
     * The Inactive phase is set when no extensions and fragments are running.
     * the ExtensionLoader is set as INACTIVE until no extensions are loaded
     * */
    INACTIVE,
    /**
     * Defines the loader to be in "Active" phase.
     * "Active" phase is set when extensions and fragments are all running
     * */
    ACTIVE,
    /**
     * Defines the loader to be in "Closed" phase.
     * Once closed, the loader can't perform any operation (throws {@link IllegalStateException}).
     * */
    CLOSE

}
