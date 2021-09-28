package fr.linkit.api.gnom.cache.sync.invokation.remote

import fr.linkit.api.gnom.cache.sync.behavior.RMIRulesAgreement
import fr.linkit.api.gnom.cache.sync.invokation.MethodInvocation

trait RemoteMethodInvocation[R] extends MethodInvocation[R] {

    val agreement: RMIRulesAgreement

}