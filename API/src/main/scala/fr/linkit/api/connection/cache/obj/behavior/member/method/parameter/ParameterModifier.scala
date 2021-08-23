package fr.linkit.api.connection.cache.obj.behavior.member.method.parameter

import fr.linkit.api.connection.cache.obj.invokation.local.LocalMethodInvocation
import fr.linkit.api.connection.network.Engine

trait ParameterModifier[P] {

    def forLocalComingFromLocal(localParam: P, originalArguments: Array[Any]): P = localParam

    def forLocalComingFromRemote(receivedParam: P, invocation: LocalMethodInvocation[_], remote: Engine): P = receivedParam

    def forRemote(localParam: P, invocation: LocalMethodInvocation[_], remote: Engine): P = localParam


}
