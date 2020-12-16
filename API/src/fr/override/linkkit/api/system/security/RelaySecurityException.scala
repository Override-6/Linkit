package fr.`override`.linkkit.api.system.security

import fr.`override`.linkkit.api.exception.RelayException

case class RelaySecurityException(msg: String) extends RelayException(msg){

}
