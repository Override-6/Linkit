package fr.linkit.api.gnom.cache.sync.behavior.member.method.parameter

import fr.linkit.api.gnom.cache.sync.invokation.local.LocalMethodInvocation
import fr.linkit.api.application.network.Engine

trait ParameterModifier[P] {

    def forLocalComingFromLocal(localParam: P, originalArguments: Array[Any]): P = localParam

    def forLocalComingFromRemote(receivedParam: P, invocation: LocalMethodInvocation[_], remote: Engine): P = receivedParam

    def forRemote(localParam: P, invocation: LocalMethodInvocation[_], remote: Engine): P = localParam


}
