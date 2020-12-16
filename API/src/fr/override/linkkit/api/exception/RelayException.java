package fr.override.linkkit.api.exception;

/**
 * thrown to report an internal incident in the Relays
 * */
public class RelayException extends Exception {

    public RelayException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public RelayException(String msg) {
        super(msg);
    }

}
