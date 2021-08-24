package fr.linkit.engine.connection.cache.obj.behavior.member

import fr.linkit.api.connection.cache.obj.behavior.member.method.parameter.{ParameterBehavior, ParameterModifier}
import org.jetbrains.annotations.Nullable

case class MethodParameterBehavior[A](paramName: String,
                                      override val isActivated: Boolean,
                                      @Nullable override val modifier: ParameterModifier[A]) extends ParameterBehavior[A] {
    override def getName: String = paramName

}
object MethodParameterBehavior {
    def apply[A](isActivated: Boolean, modifier: ParameterModifier[A]): MethodParameterBehavior[A] = new MethodParameterBehavior("", isActivated, modifier)
}