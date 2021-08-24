package fr.linkit.api.connection.cache.obj.behavior.member

import fr.linkit.api.connection.cache.obj.behavior.member.field.FieldBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.InternalMethodBehavior
import fr.linkit.api.connection.cache.obj.description.{FieldDescription, MethodDescription}
import fr.linkit.api.local.concurrency.Procrastinator

trait MemberBehaviorFactory {

    def genMethodBehavior(callProcrastinator: Option[Procrastinator], desc: MethodDescription): InternalMethodBehavior

    def genFieldBehavior(desc: FieldDescription): FieldBehavior[AnyRef]

}
