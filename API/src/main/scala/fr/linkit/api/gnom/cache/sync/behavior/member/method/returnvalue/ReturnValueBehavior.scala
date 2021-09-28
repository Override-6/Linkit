package fr.linkit.api.gnom.cache.sync.behavior.member.method.returnvalue

import fr.linkit.api.gnom.cache.sync.behavior.member.MemberBehavior

trait ReturnValueBehavior[R] extends MemberBehavior {
    val tpe     : Class[_]
    val modifier: ReturnValueModifier[R]
}