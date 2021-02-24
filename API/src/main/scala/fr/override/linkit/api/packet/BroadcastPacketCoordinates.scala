package fr.`override`.linkit.api.packet

import fr.`override`.linkit.api.packet.serialization.ObjectSerializer

case class BroadcastPacketCoordinates(injectableID: Int, senderID: String, discardTargets: Boolean, targetIDs: String*) extends PacketCoordinates {
    override def determineSerializer(array: Array[String], raw: ObjectSerializer, cached: ObjectSerializer): ObjectSerializer = {
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
