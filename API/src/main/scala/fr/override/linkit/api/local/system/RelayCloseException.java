package fr.override.linkit.api.local.system;

/**
 * thrown to report an internal incident in the Relays
 * */
public class RelayCloseException extends RelayException {

    public RelayCloseException(String msg) {
        super(msg);
    }

}
