package fr.overridescala.vps.ftp.api.system;

public enum Reason {
    LOCAL_ERROR(true), EXTERNAL_ERROR(false), LOCAL(true), EXTERNAL(false);

    private final boolean isLocal;

    Reason(boolean isLocal) {
        this.isLocal = isLocal;
    }

    public boolean isLocal() {
        return isLocal;
    }
}