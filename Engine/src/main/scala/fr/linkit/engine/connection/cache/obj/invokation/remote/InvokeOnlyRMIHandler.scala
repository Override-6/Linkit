package fr.linkit.engine.connection.cache.obj.invokation.remote

import fr.linkit.api.connection.cache.obj.behavior.RMIRulesAgreement
import fr.linkit.api.connection.cache.obj.invokation.remote.{Puppeteer, SynchronizedMethodInvocation}

object InvokeOnlyRMIHandler extends AbstractMethodInvocationHandler {

    override def voidRMIInvocation(puppeteer: Puppeteer[_], agreement: RMIRulesAgreement, invocation: SynchronizedMethodInvocation[_]): Unit = {
        puppeteer.sendInvoke(agreement, invocation)
    }
}
