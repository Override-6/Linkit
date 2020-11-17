package fr.overridescala.vps.ftp.api.exceptions;

/**
 * thrown to report an internal incident in the Relays
 * */
public class RelayClosedException extends RelayException {

    public RelayClosedException(String msg) {
        super(msg);
    }

}
