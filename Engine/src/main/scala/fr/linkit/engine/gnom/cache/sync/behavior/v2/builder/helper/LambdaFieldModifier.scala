package fr.linkit.engine.gnom.cache.sync.behavior.v2.builder.helper

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.behavior.member.field.FieldModifier
import fr.linkit.api.gnom.network.Engine

abstract class LambdaFieldModifier[F <: AnyRef] extends FieldModifier[F] {
    protected var fromRemote     : (F, SynchronizedObject[_], Engine) => F    = (f, _, _) => f
    protected var fromRemoteEvent: (F, SynchronizedObject[_], Engine) => Unit = (f, _, _) => ()

    override final def receivedFromRemote(receivedField: F, containingObject: SynchronizedObject[_], remote: Engine): F = {
        fromRemote(receivedField, containingObject, remote)
    }

    override final def receivedFromRemoteEvent(receivedField: F, containingObject: SynchronizedObject[_], remote: Engine): Unit = {
        fromRemoteEvent(receivedField, containingObject, remote)
    }
}
