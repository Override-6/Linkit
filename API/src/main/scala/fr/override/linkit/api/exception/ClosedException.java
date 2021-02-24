package fr.override.linkit.api.exception;

/**
 * thrown to report an internal incident in the Relays
 * */
public class ClosedException extends RelayException {

    public ClosedException(String msg) {
        super(msg);
    }

}
