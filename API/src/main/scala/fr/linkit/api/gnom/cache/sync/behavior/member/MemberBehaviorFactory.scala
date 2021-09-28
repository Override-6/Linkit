package fr.linkit.api.gnom.cache.sync.behavior.member

import fr.linkit.api.gnom.cache.sync.behavior.member.field.FieldBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.InternalMethodBehavior
import fr.linkit.api.gnom.cache.sync.description.{FieldDescription, MethodDescription}
import fr.linkit.api.internal.concurrency.Procrastinator

trait MemberBehaviorFactory {

    def genMethodBehavior(callProcrastinator: Option[Procrastinator], desc: MethodDescription): InternalMethodBehavior

    def genFieldBehavior(desc: FieldDescription): FieldBehavior[AnyRef]

}
