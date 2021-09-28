package fr.linkit.api.connection.network.reference
import fr.linkit.api.connection.reference.ReferencedObjectLocation

trait ReferencedObjectStore {


    def findLocation(ref: AnyRef): Option[Int]

    def isPresent(l: Int): Boolean

    def findObject(location: ReferencedObjectLocation): Option[AnyRef]
}
