package fr.override.linkit.api.connection.task;

import fr.override.linkit.api.local.system.RelayException;

/**
 * thrown to report an internal incident in the Tasks
 */
public class TaskException extends RelayException {

    public TaskException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public TaskException(String msg) {
        super(msg);
    }

}
