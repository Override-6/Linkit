package fr.overridescala.vps.ftp.api.exceptions;

import fr.overridescala.vps.ftp.api.task.DynamicTaskCompleterFactory;

public class RelayException extends Exception {

    public RelayException(String msg) {
        super(msg);
    }

}
