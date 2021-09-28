package fr.linkit.api.gnom.cache.sync.invokation.remote

trait DispatchableRemoteMethodInvocation[R] extends RemoteMethodInvocation[R] {
    def dispatchRMI(dispatcher: Puppeteer[AnyRef]#RMIDispatcher)
}
