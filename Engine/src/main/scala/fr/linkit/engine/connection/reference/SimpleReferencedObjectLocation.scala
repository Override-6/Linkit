package fr.linkit.engine.connection.reference

import fr.linkit.api.connection.reference.ReferencedObjectLocation

class SimpleReferencedObjectLocation(override val originChannelPath: Array[Int],
                                     override val refCode: Int) extends ReferencedObjectLocation {

}
