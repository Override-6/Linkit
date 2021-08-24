package fr.linkit.api.connection.cache.obj.behavior.member.method.returnvalue

import fr.linkit.api.connection.cache.obj.behavior.member.MemberBehavior

trait ReturnValueBehavior[R] extends MemberBehavior {
    val tpe     : Class[_]
    val modifier: ReturnValueModifier[R]
}