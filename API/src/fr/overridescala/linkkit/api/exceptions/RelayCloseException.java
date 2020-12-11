package fr.overridescala.linkkit.api.exceptions;

/**
 * thrown to report an internal incident in the Relays
 * */
public class RelayCloseException extends RelayException {

    public RelayCloseException(String msg) {
        super(msg);
    }

}
