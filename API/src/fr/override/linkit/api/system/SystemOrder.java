package fr.override.linkit.api.system;

public enum SystemOrder {
    CLIENT_CLOSE,
    SERVER_CLOSE,
    ABORT_TASK,
    CHECK_ID,

    /**
     * @deprecated Use api.network to retrieve more information, this is more safe and much more efficient
     */
    @Deprecated
    PRINT_INFO
}