package fr.linkit.api.gnom.reference

trait ReferencedObjectLocation extends NetworkReferenceLocation[AnyRef] {

    val originChannelPath: Array[Int]

    val refCode: Int

}
