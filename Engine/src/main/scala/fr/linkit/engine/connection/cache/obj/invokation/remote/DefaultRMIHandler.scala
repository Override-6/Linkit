package fr.linkit.engine.connection.cache.obj.invokation.remote

import fr.linkit.api.connection.cache.obj.behavior.RMIRulesAgreement
import fr.linkit.api.connection.cache.obj.invokation.MethodInvocation
import fr.linkit.api.connection.cache.obj.invokation.remote.Puppeteer

object DefaultRMIHandler extends AbstractMethodInvocationHandler {

    override def voidRMIInvocation(puppeteer: Puppeteer[_], agreement: RMIRulesAgreement, invocation: MethodInvocation[_]): Unit = {
        puppeteer.sendInvokeAndWaitResult(agreement, invocation)
    }
}
