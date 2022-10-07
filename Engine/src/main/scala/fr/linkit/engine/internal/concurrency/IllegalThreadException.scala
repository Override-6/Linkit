package fr.linkit.engine.internal.concurrency

import fr.linkit.api.internal.system.AppException

class IllegalThreadException(msg: String, cause: Throwable = null) extends AppException(msg, cause)