package fr.linkit.engine.internal.debug

import fr.linkit.api.gnom.cache.sync.ConnectedObjectReference
import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIRulesAgreement
import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.{SharedCache, SharedCacheReference}
import fr.linkit.api.gnom.persistence.obj.TrafficReference
import fr.linkit.api.internal.concurrency.Worker

sealed trait State {
    def actionType: String

    def insights: String

    def taskPath: Array[Int]

}

sealed abstract class AbstractState(val actionType: String) extends State {
    override val taskPath = Thread.currentThread() match {
        case w: Worker => w.getTaskStack
        case _         => Array()
    }
}

case class RequestState(requestType: String, goal: String, targetEngine: String, channel: TrafficReference) extends AbstractState("request") {
    override def insights: String = s"Performing request '$requestType' in channel $channel with goal: $goal. waiting $targetEngine to answer..."
}

case class ResponseState(requestType: String, senderEngine: String, channel: TrafficReference) extends AbstractState("response") {
    override def insights: String = s"Performing response to request '$requestType' in channel $channel. engine '$senderEngine' is currently waiting answer..."
}

case class MethodInvocationSendState(agreement: RMIRulesAgreement, method: MethodDescription, appointedEngine: String) extends AbstractState("rmi - send") {
    override def insights: String = s"Performing remote method invocation following agreement $agreement, for method $method." + {
        if (appointedEngine == null) "" else s" waiting for engine '$appointedEngine' to return or throw."
    }
}

case class MethodInvocationComputeState(methodID: Int, triggerEngine: String, isWaiting: Boolean) extends AbstractState("rmi - compute") {
    override def insights: String = s"Computing method invocation (id $methodID) triggered by $triggerEngine." + {
        if (!isWaiting) "" else s" remote is waiting this method invocation to return or throw."
    }
}

case class MethodInvocationExecutionState(method: MethodDescription, triggerEngine: String, isWaiting: Boolean) extends AbstractState("rmi - execute") {
    override def insights: String = s"Executing method $method triggered by $triggerEngine." + {
        if (!isWaiting) "" else s" remote is waiting this method to return or throw."
    }
}

case class PacketInjectionState(packetID: String, channel: TrafficReference) extends AbstractState("injection") {
    override def insights: String = s"Injecting packet $packetID into channel $channel."
}

case class TaskPausedState(taskID: Int, timeout: Long = 0) extends AbstractState("task pause") {
    override def insights: String = s"Task $taskID is paused." + (if (timeout > 0) " timeout = " + timeout else "")
}

case class SIPURectifyState(channel: TrafficReference, currentOrdinal: Int, expectedOrdinal: Int) extends AbstractState("SIPU - rectify") {
    override def insights: String = {
        val diff = currentOrdinal - expectedOrdinal
        s"Waiting remaining packets for channel $channel. SIPU's Head of queue ordinal is $diff ahead expected ordinal of $expectedOrdinal."
    }
}

case class PacketSerializationState(packetID: String, target: String) extends AbstractState("serialize") {
    override def insights: String = s"Serializing packet '$packetID' for targeted engine $target."
}

case class PacketDeserializationState(packetID: String, sender: String) extends AbstractState("deserialize") {
    override def insights: String = s"Deserializing packet '$packetID' sent from engine $sender."
}

case class ConnectedObjectTreeRetrievalState(reference: ConnectedObjectReference) extends AbstractState("network object retrieval") {
    override def insights: String = s"Retrieving network object tree $reference..."
}

case class ConnectedObjectCreationState(objectReference: ConnectedObjectReference) extends AbstractState("connected object creation") {
    override def insights: String = s"Creating connected object $objectReference."
}

case class CacheOpenState(cacheReference: SharedCacheReference, cacheType: Class[_ <: SharedCache]) extends AbstractState("cache open") {
    override def insights: String = s"opening cache of type '${cacheType.getName}' at $cacheReference."
}