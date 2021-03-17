package fr.override.linkit.skull.internal.system;

/**
 * thrown to report an internal incident in the Relays
 * */
public class RelayCloseException extends RelayException {

    public RelayCloseException(String msg) {
        super(msg);
    }

}
