package fr.linkit.api.gnom.cache.sync.contract

import fr.linkit.api.gnom.cache.sync.contract.behavior.member.method.ParameterBehavior
import fr.linkit.api.gnom.cache.sync.contract.modification.MethodCompModifier

import java.lang.reflect.Parameter

trait ParameterContract[P] {

    val param   : Parameter
    val behavior: Option[ParameterBehavior[P]]
    val modifier: Option[MethodCompModifier[P]]

}
