package fr.linkit.api.connection.cache.obj.behavior.member.method

import fr.linkit.api.connection.cache.obj.behavior.{RMIRulesAgreement, RMIRulesAgreementBuilder}
import fr.linkit.api.connection.cache.obj.invokation.local.LocalMethodInvocation
import fr.linkit.api.connection.cache.obj.invokation.remote.Puppeteer
import fr.linkit.api.connection.network.Network

trait InternalMethodBehavior extends MethodBehavior {

    def completeAgreement(builder: RMIRulesAgreementBuilder): RMIRulesAgreement

    def dispatch(dispatcher: Puppeteer[AnyRef]#RMIDispatcher, network: Network, invocation: LocalMethodInvocation[_]): Unit
}


