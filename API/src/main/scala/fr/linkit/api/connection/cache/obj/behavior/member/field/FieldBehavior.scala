package fr.linkit.api.connection.cache.obj.behavior.member.field

import fr.linkit.api.connection.cache.obj.behavior.member.MemberBehavior
import fr.linkit.api.connection.cache.obj.description.FieldDescription

trait FieldBehavior[A] extends MemberBehavior {

    val desc: FieldDescription

    val modifier: FieldModifier[A]

}
