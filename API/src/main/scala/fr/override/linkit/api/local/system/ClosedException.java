package fr.override.linkit.api.local.system;

/**
 * thrown to report an internal incident in the Relays
 * */
public class ClosedException extends AppException {

    public ClosedException(String msg) {
        super(msg);
    }

}
