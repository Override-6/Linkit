package fr.linkit.api.gnom.cache.sync.behavior.member.field

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.application.network.Engine

trait FieldModifier[F] {

    def forLocalComingFromLocal(localField: F, containingObject: SynchronizedObject[_]): F = localField

    def forLocalComingFromRemote(receivedField: F, containingObject: SynchronizedObject[_], remote: Engine): F = receivedField

}
