package fr.override.linkit.api.connection.task;

import fr.override.linkit.api.local.system.AppException;

/**
 * thrown to report an internal incident in the Tasks
 */
public class TaskException extends AppException {

    public TaskException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public TaskException(String msg) {
        super(msg);
    }

}
