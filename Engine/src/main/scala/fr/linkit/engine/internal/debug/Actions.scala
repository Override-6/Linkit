package fr.linkit.engine.internal.debug

import fr.linkit.api.gnom.cache.sync.ConnectedObjectReference
import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIRulesAgreement
import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.{SharedCache, SharedCacheReference}
import fr.linkit.api.gnom.persistence.obj.TrafficReference
import fr.linkit.api.internal.concurrency.Worker

sealed trait Action {
    def actionType: String

    def insights: String

    def taskPath: Array[Int]

}

sealed abstract class AbstractAction(val actionType: String) extends Action {
    override val taskPath = Thread.currentThread() match {
        case w: Worker => w.getTaskStack
        case _         => Array()
    }
}

case class RequestAction(requestType: String, goal: String, targetEngine: String, channel: TrafficReference) extends AbstractAction("request") {
    override def insights: String = s"Performing request '$requestType' in channel $channel with goal: $goal. waiting $targetEngine to answer..."
}

case class ResponseAction(requestType: String, senderEngine: String, channel: TrafficReference) extends AbstractAction("response") {
    override def insights: String = s"Performing response to request '$requestType' in channel $channel. engine '$senderEngine' is currently waiting answer..."
}

case class MethodInvocationSendAction(agreement: RMIRulesAgreement, method: MethodDescription, appointedEngine: String) extends AbstractAction("rmi - send") {
    override def insights: String = s"Performing remote method invocation following agreement $agreement, for method $method." + {
        if (appointedEngine == null) "" else s" waiting for engine '$appointedEngine' to return or throw."
    }
}

case class MethodInvocationComputeAction(methodID: Int, triggerEngine: String, isWaiting: Boolean) extends AbstractAction("rmi - compute") {
    override def insights: String = s"Computing method invocation (id $methodID) triggered by $triggerEngine." + {
        if (!isWaiting) "" else s" remote is waiting this method invocation to return or throw."
    }
}

case class MethodInvocationExecutionAction(methodID: Int, triggerEngine: String, isWaiting: Boolean) extends AbstractAction("rmi - execute") {
    override def insights: String = s"Executing method (id $methodID) triggered by $triggerEngine." + {
        if (!isWaiting) "" else s" remote is waiting this method to return or throw."
    }
}

case class PacketInjectionAction(packetID: String, channel: TrafficReference) extends AbstractAction("injection") {
    override def insights: String = s"Injecting packet $packetID into channel $channel."
}

case class TaskPausedAction(taskID: Int, timeout: Long = 0) extends AbstractAction("task pause") {
    override def insights: String = s"Task $taskID is paused." + (if (timeout > 0) " timeout = " + timeout else "")
}

case class SIPURectifyAction(channel: TrafficReference, currentOrdinal: Int, expectedOrdinal: Int) extends AbstractAction("SIPU - rectify") {
    override def insights: String = {
        val diff = currentOrdinal - expectedOrdinal
        s"Waiting remaining packets for channel $channel. SIPU's Head of queue ordinal is $diff ahead expected ordinal of $expectedOrdinal."
    }
}

case class PacketSerializationAction(packetID: String, target: String) extends AbstractAction("serialize") {
    override def insights: String = s"Serializing packet '$packetID' for targeted engine $target."
}

case class PacketDeserializationAction(packetID: String, sender: String) extends AbstractAction("deserialize") {
    override def insights: String = s"Deserializing packet '$packetID' sent from engine $sender."
}

case class ConnectedObjectTreeRetrievalAction(reference: ConnectedObjectReference) extends AbstractAction("network object retrieval") {
    override def insights: String = s"Retrieving network object tree $reference..."
}

case class ConnectedObjectCreationAction(objectReference: ConnectedObjectReference) extends AbstractAction("connected object creation") {
    override def insights: String = s"Creating connected object $objectReference."
}

case class CacheOpenAction(cacheReference: SharedCacheReference, cacheType: Class[_ <: SharedCache]) extends AbstractAction("cache open") {
    override def insights: String = s"opening cache of type '${cacheType.getName}' at $cacheReference."
}