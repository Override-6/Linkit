package fr.linkit.engine.connection.reference

import fr.linkit.api.connection.reference.{NetworkReferenceLocation, ObjectManagersLinker}

class NetworkObjectsLinker(referenceManager: ReferencedObjectsManager, ) extends ObjectManagersLinker {
    override def getObject[R <: AnyRef](location: NetworkReferenceLocation[R]): R = {

    }

    override def getLocation[R <: AnyRef](ref: R): NetworkReferenceLocation[R] = ???
}
