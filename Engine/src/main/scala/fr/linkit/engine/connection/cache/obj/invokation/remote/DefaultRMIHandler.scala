package fr.linkit.engine.connection.cache.obj.invokation.remote

import fr.linkit.api.connection.cache.obj.invokation.remote.{DispatchableRemoteMethodInvocation, Puppeteer}

object DefaultRMIHandler extends AbstractMethodInvocationHandler {

    override def voidRMIInvocation(puppeteer: Puppeteer[_], invocation: DispatchableRemoteMethodInvocation[_]): Unit = {
        puppeteer.sendInvoke(invocation)
    }
}
