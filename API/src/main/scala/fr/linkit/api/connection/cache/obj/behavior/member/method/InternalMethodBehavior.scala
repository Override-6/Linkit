package fr.linkit.api.connection.cache.obj.behavior.member.method

import fr.linkit.api.connection.cache.obj.behavior.{ObjectBehaviorStore, RMIRulesAgreement, RMIRulesAgreementBuilder}
import fr.linkit.api.connection.cache.obj.invokation.local.LocalMethodInvocation
import fr.linkit.api.connection.cache.obj.invokation.remote.Puppeteer
import fr.linkit.api.connection.network.Network

trait InternalMethodBehavior extends MethodBehavior {

    def completeAgreement(builder: RMIRulesAgreementBuilder): RMIRulesAgreement


    /**
     * uses the given RMIDispatcher
     *
     * @param dispatcher the dispatcher to use
     */
    def dispatch(dispatcher: Puppeteer[AnyRef]#RMIDispatcher, network: Network, store: ObjectBehaviorStore, invocation: LocalMethodInvocation[_]): Unit
}


