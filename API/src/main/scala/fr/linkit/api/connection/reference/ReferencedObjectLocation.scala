package fr.linkit.api.connection.reference

trait ReferencedObjectLocation extends NetworkReferenceLocation[AnyRef] {

    val originChannelPath: Array[Int]

    val refCode: Int

}
