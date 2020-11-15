package fr.overridescala.vps.ftp.api.exceptions

/**
 * Must be only thrown during task executions.
 * once catched, both Relay Point and Relay Server will only print the message exception
 * */
class TaskOperationFailException(msg: String) extends TaskException(msg)
