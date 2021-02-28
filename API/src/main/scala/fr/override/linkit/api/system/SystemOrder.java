package fr.override.linkit.api.system;

public enum SystemOrder {
    CLIENT_CLOSE,
    SERVER_CLOSE,
    ABORT_TASK,
    /**
     * @deprecated use the api.network.Network interface to check identifiers
     */
    @Deprecated
    CHECK_ID
}