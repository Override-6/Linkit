package fr.linkit.api.connection.cache.obj.invokation.remote

import fr.linkit.api.connection.cache.obj.behavior.RMIRulesAgreement
import fr.linkit.api.connection.cache.obj.invokation.MethodInvocation

trait RemoteMethodInvocation[R] extends MethodInvocation[R] {

    val agreement: RMIRulesAgreement

    def dispatchRMI(dispatcher: Puppeteer[_]#RMIDispatcher)

}
