package fr.linkit.api.internal.compilation

import fr.linkit.api.internal.system.AppException

class RuntimeCompilationException(msg: String, cause: Throwable = null) extends AppException(msg, cause) {

}
