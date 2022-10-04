package fr.linkit.engine.internal.concurrency.pool

import fr.linkit.api.internal.system.AppException

class WorkerException(msg: String, cause: Throwable = null) extends AppException(msg, cause)