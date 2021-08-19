package fr.linkit.api.connection.cache.obj.behavior.member

import org.jetbrains.annotations.Nullable

case class MethodParameterBehavior[A](isSynchronized: Boolean,
                                      @Nullable remoteParamModifier: RemoteParameterModifier[A],
                                      @Nullable localParamModifier: LocalParameterModifier[A]) {
}
object MethodParameterBehavior {
    val Deactivated: MethodParameterBehavior[_] = MethodParameterBehavior[Any](false, null, null)
}
