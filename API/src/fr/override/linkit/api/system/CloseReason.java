package fr.override.linkit.api.system;

//TODO Add more Detailed Reason, may have a Reason interface, and a CloseReason Enum
public enum CloseReason {
    INTERNAL(true, false),
    INTERNAL_ERROR(true, false),

    EXTERNAL_ERROR(false, true),
    EXTERNAL(false, true),

    SECURITY_CHECK(false, false),
    NOT_SPECIFIED(false, false);

    private final boolean isInternal;
    private final boolean isExternal;

    CloseReason(boolean isInternal, boolean isExternal) {
        this.isInternal = isInternal;
        this.isExternal = isExternal;
    }

    public boolean isInternal() {
        return isInternal;
    }

    public boolean isExternal() {
        return isExternal;
    }



    public CloseReason reversedPOV() {
        switch (this) {
            case INTERNAL: return EXTERNAL;
            case INTERNAL_ERROR: return EXTERNAL_ERROR;

            case EXTERNAL: return INTERNAL;
            case EXTERNAL_ERROR: return INTERNAL_ERROR;

            default: return this;
        }
    }

}