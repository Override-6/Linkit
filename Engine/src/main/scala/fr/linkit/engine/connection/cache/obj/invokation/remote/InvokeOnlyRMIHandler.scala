package fr.linkit.engine.connection.cache.obj.invokation.remote

import fr.linkit.api.connection.cache.obj.invokation.remote.{Puppeteer, RemoteMethodInvocation}

object InvokeOnlyRMIHandler extends AbstractMethodInvocationHandler {

    override def voidRMIInvocation(puppeteer: Puppeteer[_], invocation: RemoteMethodInvocation[_]): Unit = {
        puppeteer.sendInvoke(invocation)
    }
}
