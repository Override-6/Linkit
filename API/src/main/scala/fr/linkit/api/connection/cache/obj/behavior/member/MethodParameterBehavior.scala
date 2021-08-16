package fr.linkit.api.connection.cache.obj.behavior.member

import org.jetbrains.annotations.Nullable

case class MethodParameterBehavior[A](isSynchronized: Boolean, @Nullable modifier: ParameterModifier[A]) {


}
