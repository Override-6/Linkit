package fr.linkit.api.gnom.cache.sync.behavior.member.field

import fr.linkit.api.gnom.cache.sync.behavior.member.MemberBehavior
import fr.linkit.api.gnom.cache.sync.description.FieldDescription

trait FieldBehavior[A] extends MemberBehavior {

    val desc: FieldDescription

    val modifier: FieldModifier[A]

}
