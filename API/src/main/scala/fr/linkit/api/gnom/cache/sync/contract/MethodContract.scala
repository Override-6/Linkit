package fr.linkit.api.gnom.cache.sync.contract

import fr.linkit.api.gnom.cache.sync.contract.behavior.member.method.UsageMethodBehavior
import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.contractv2.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.invocation.remote.MethodInvocationHandler
import fr.linkit.api.internal.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable

trait MethodContract {

              val description        : MethodDescription
              val behavior           : UsageMethodBehavior
              val parameterContracts : Array[ParameterContract[Any]]
    @Nullable val returnValueModifier: ValueModifier[Any]
    @Nullable val procrastinator     : Procrastinator
    @Nullable val handler            : MethodInvocationHandler

}
