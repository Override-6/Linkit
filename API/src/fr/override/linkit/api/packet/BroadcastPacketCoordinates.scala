package fr.`override`.linkit.api.packet

import fr.`override`.linkit.api.packet.serialization.PacketSerializer

case class BroadcastPacketCoordinates(injectableID: Int, senderID: String, discardTargets: Boolean, targetsID: String*) extends PacketCoordinates {
    override def determineSerializer(array: Array[String], raw: PacketSerializer, cached: PacketSerializer): PacketSerializer = {
        //if there is a target that is not whitelisted, use the raw serializer
        if (targetsID.exists(!array.contains(_)))
            raw
        else cached
    }

    def listDiscarded(alreadyConnected: Seq[String]): Seq[String] = {
        if (discardTargets)
            targetsID
        else alreadyConnected.filterNot(targetsID.contains)
    }

    override def toString: String = s"BroadcastPacketCoordinates(injectableID: $injectableID, senderID: $senderID, discardTargets: $discardTargets, targetIDs: $targetsID)"
}
