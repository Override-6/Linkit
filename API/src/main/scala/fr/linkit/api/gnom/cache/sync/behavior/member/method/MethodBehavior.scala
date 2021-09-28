package fr.linkit.api.gnom.cache.sync.behavior.member.method

import fr.linkit.api.gnom.cache.sync.behavior.member.MemberBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.parameter.ParameterBehavior
import fr.linkit.api.gnom.cache.sync.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.invokation.remote.MethodInvocationHandler
import fr.linkit.api.internal.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable

trait MethodBehavior extends MemberBehavior {

              val desc              : MethodDescription
              val parameterBehaviors: Array[ParameterBehavior[AnyRef]]
              val syncReturnValue   : Boolean
              val isHidden          : Boolean
              val innerInvocations  : Boolean
              val defaultReturnValue: Any
    @Nullable val procrastinator    : Procrastinator
    @Nullable val handler           : MethodInvocationHandler

}
