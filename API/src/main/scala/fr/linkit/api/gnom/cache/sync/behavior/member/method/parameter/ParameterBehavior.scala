package fr.linkit.api.gnom.cache.sync.behavior.member.method.parameter

import fr.linkit.api.gnom.cache.sync.behavior.member.MemberBehavior
import org.jetbrains.annotations.Nullable

import java.lang.reflect.Parameter

trait ParameterBehavior[A] extends MemberBehavior {

    val param: Parameter
    @Nullable val modifier: ParameterModifier[A]

}
