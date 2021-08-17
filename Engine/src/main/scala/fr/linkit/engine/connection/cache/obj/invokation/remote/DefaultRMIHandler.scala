package fr.linkit.engine.connection.cache.obj.invokation.remote

import fr.linkit.api.connection.cache.obj.behavior.RMIRulesAgreement
import fr.linkit.api.connection.cache.obj.invokation.MethodInvocation
import fr.linkit.api.connection.cache.obj.invokation.remote.{Puppeteer, RemoteMethodInvocation}

object DefaultRMIHandler extends AbstractMethodInvocationHandler {

    override def voidRMIInvocation(puppeteer: Puppeteer[_], invocation: RemoteMethodInvocation[_]): Unit = {
        puppeteer.sendInvokeAndWaitResult(invocation)
    }
}
