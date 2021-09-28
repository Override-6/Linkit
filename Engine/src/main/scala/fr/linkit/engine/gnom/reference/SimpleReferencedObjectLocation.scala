package fr.linkit.engine.gnom.reference

import fr.linkit.api.gnom.reference.ReferencedObjectLocation

class SimpleReferencedObjectLocation(override val originChannelPath: Array[Int],
                                     override val refCode: Int) extends ReferencedObjectLocation {

}
