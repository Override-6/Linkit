package fr.override.linkit.api.local.system;

/**
 * thrown to report an internal incident in the Relays
 * */
public class AppException extends Exception {

    public AppException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public AppException(String msg) {
        super(msg);
    }

}
