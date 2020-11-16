package fr.overridescala.vps.ftp.api.system;

public enum Reason {
    INTERNAL_ERROR(true), EXTERNAL_ERROR(false), INTERNAL(true), EXTERNAL(false);

    private final boolean isLocal;

    Reason(boolean isLocal) {
        this.isLocal = isLocal;
    }

    public boolean isInternal() {
        return isLocal;
    }

    public Reason reversed() {
        switch (this) {
            case INTERNAL: return EXTERNAL;
            case INTERNAL_ERROR: return EXTERNAL_ERROR;
            case EXTERNAL: return INTERNAL;
            case EXTERNAL_ERROR: return INTERNAL_ERROR;
            default: throw new UnknownError();
        }
    }

}