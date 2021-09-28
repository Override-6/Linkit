package fr.linkit.engine.gnom.reference

import fr.linkit.api.gnom.reference.{NetworkReferenceLocation, ObjectManagersLinker}

class NetworkObjectsLinker(referenceManager: ReferencedObjectsManager) extends ObjectManagersLinker {
    override def getObject[R <: AnyRef](location: NetworkReferenceLocation[R]): R = {
???
    }

    override def getLocation[R <: AnyRef](ref: R): NetworkReferenceLocation[R] = ???
}
