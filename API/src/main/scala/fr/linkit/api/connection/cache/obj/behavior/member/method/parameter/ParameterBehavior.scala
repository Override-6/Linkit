package fr.linkit.api.connection.cache.obj.behavior.member.method.parameter

import fr.linkit.api.connection.cache.obj.behavior.member.MemberBehavior
import org.jetbrains.annotations.Nullable

trait ParameterBehavior[A] extends MemberBehavior {

    @Nullable val modifier: ParameterModifier[A]

}
