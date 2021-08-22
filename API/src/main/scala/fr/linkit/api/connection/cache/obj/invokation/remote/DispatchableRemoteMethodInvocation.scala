package fr.linkit.api.connection.cache.obj.invokation.remote

trait DispatchableRemoteMethodInvocation[R] extends RemoteMethodInvocation[R] {
    def dispatchRMI(dispatcher: Puppeteer[AnyRef]#RMIDispatcher)
}
