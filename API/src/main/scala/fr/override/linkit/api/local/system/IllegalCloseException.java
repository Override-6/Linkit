package fr.override.linkit.api.local.system;

/**
 * thrown to report an internal incident in the Relays
 * */
public class IllegalCloseException extends AppException {

    public IllegalCloseException(String msg) {
        super(msg);
    }

}
