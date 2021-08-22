package fr.linkit.api.connection.cache.obj.behavior.member.method

import fr.linkit.api.connection.cache.obj.behavior.{RMIRulesAgreement, RMIRulesAgreementBuilder}
import fr.linkit.api.connection.cache.obj.behavior.member.MemberBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.parameter.ParameterBehavior
import fr.linkit.api.connection.cache.obj.description.MethodDescription
import fr.linkit.api.connection.cache.obj.invokation.remote.MethodInvocationHandler
import fr.linkit.api.local.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable

trait MethodBehavior extends MemberBehavior {

              val desc              : MethodDescription
              val parameterBehaviors: Array[ParameterBehavior[Any]]
              val syncReturnValue   : Boolean
              val isHidden          : Boolean
              val defaultReturnValue: Any
    @Nullable val procrastinator    : Procrastinator
    @Nullable val handler           : MethodInvocationHandler


}
