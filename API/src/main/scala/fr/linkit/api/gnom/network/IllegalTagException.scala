package fr.linkit.api.gnom.network

import fr.linkit.api.internal.system.AppException

class IllegalTagException(msg: String, cause: Throwable = null) extends AppException(msg, cause)