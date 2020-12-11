package fr.overridescala.linkkit.api.system.security

import fr.overridescala.linkkit.api.exceptions.RelayException

case class RelaySecurityException(msg: String) extends RelayException(msg){

}
