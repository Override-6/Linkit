package fr.linkit.engine.connection.cache.obj.behavior.member

import fr.linkit.api.connection.cache.obj.behavior.member.method.parameter.{ParameterBehavior, ParameterModifier}
import org.jetbrains.annotations.Nullable

import java.lang.reflect.Parameter

case class MethodParameterBehavior[A](override val param: Parameter,
                                      override val isActivated: Boolean,
                                      @Nullable override val modifier: ParameterModifier[A]) extends ParameterBehavior[A] {
    override def getName: String = param.getName

}