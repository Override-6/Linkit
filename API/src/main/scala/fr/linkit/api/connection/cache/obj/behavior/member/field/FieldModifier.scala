package fr.linkit.api.connection.cache.obj.behavior.member.field

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.network.Engine

trait FieldModifier[F] {

    def forLocalComingFromLocal(localField: F, containingObject: SynchronizedObject[_]): F = localField

    def forLocalComingFromRemote(receivedField: F, containingObject: SynchronizedObject[_], remote: Engine): F = receivedField

}
