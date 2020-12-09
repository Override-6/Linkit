package fr.overridescala.linkkit.api.exceptions;

/**
 * thrown to report an internal incident in the Tasks
 * */
public class TaskException extends RelayException {

    public TaskException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public TaskException(String msg) {
        super(msg);
    }

}
