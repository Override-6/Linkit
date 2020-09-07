package fr.overridescala.vps.ftp.api.exceptions;

public class TransferException extends RelayException {

    public TransferException(String exceptionCause) {
        super(exceptionCause);
    }

}
