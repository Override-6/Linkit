package fr.overridescala.linkkit.api.exceptions;

/**
 * thrown to report an internal incident in the Relays
 * */
public class RelayException extends Exception {

    public RelayException(String msg) {
        super(msg);
    }

}
