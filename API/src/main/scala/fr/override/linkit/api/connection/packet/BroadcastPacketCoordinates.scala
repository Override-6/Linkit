package fr.`override`.linkit.api.connection.packet

import fr.`override`.linkit.api.connection.packet.serialization.Serializer

case class BroadcastPacketCoordinates(injectableID: Int, senderID: String, discardTargets: Boolean, targetIDs: String*) extends PacketCoordinates {

    override def determineSerializer(array: Array[String], raw: Serializer, cached: Serializer): Serializer = {
        //if there is a target that is not whitelisted, use the raw serializer
        if (targetIDs.forall(array.contains(_)))
            cached
        else raw
    }

    def listDiscarded(alreadyConnected: Seq[String]): Seq[String] = {
        if (discardTargets)
            targetIDs
        else alreadyConnected.filterNot(targetIDs.contains)
    }

    override def toString: String = s"BroadcastPacketCoordinates(injectableID: $injectableID, senderID: $senderID, discardTargets: $discardTargets, targetIDs: $targetIDs)"
}
