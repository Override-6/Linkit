package fr.linkit.api.gnom.cache.sync.behavior.member.method.returnvalue

import fr.linkit.api.gnom.cache.sync.invokation.local.LocalMethodInvocation
import fr.linkit.api.gnom.network.Engine

trait ReturnValueModifier[R] {
    def forLocalComingFromLocal(localParam: R, invocation: LocalMethodInvocation[_]): R

    def forLocalComingFromRemote(receivedParam: R, invocation: LocalMethodInvocation[_], remote: Engine): R

    def forRemote(localParam: R, invocation: LocalMethodInvocation[_], remote: Engine): R

}
