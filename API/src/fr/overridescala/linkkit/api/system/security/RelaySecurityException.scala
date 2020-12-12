package fr.overridescala.linkkit.api.system.security

import fr.overridescala.linkkit.api.exception.RelayException

case class RelaySecurityException(msg: String) extends RelayException(msg){

}
