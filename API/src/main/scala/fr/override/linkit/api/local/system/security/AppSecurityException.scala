package fr.`override`.linkit.api.local.system.security

import fr.`override`.linkit.api.local.system.AppException

case class AppSecurityException(msg: String, cause: Throwable = null) extends AppException(msg, cause){

}
