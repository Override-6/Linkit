package fr.linkit.api.connection.cache.obj.behavior.member.method.returnvalue

import fr.linkit.api.connection.cache.obj.invokation.local.LocalMethodInvocation
import fr.linkit.api.connection.network.Engine

trait ReturnValueModifier[R] {
    def forLocalComingFromLocal(localParam: R, invocation: LocalMethodInvocation[_]): R

    def forLocalComingFromRemote(receivedParam: R, invocation: LocalMethodInvocation[_], remote: Engine): R

    def forRemote(localParam: R, invocation: LocalMethodInvocation[_], remote: Engine): R

}
