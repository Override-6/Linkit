package fr.linkit.api.gnom.cache.sync.behavior.member.method

import fr.linkit.api.gnom.cache.sync.behavior.{ObjectBehaviorStore, RMIRulesAgreement, RMIRulesAgreementBuilder}
import fr.linkit.api.gnom.cache.sync.invokation.local.LocalMethodInvocation
import fr.linkit.api.gnom.cache.sync.invokation.remote.Puppeteer
import fr.linkit.api.application.network.Network

trait InternalMethodBehavior extends MethodBehavior {

    def completeAgreement(builder: RMIRulesAgreementBuilder): RMIRulesAgreement


    /**
     * uses the given RMIDispatcher
     *
     * @param dispatcher the dispatcher to use
     */
    def dispatch(dispatcher: Puppeteer[AnyRef]#RMIDispatcher, network: Network, store: ObjectBehaviorStore, invocation: LocalMethodInvocation[_]): Unit
}


