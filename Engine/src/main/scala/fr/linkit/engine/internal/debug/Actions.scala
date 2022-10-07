package fr.linkit.engine.internal.debug

import fr.linkit.api.gnom.cache.sync.ConnectedObjectReference
import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIRulesAgreement
import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.{SharedCache, SharedCacheReference}
import fr.linkit.api.gnom.persistence.obj.TrafficReference
import fr.linkit.api.internal.concurrency.Worker

sealed trait Step {
    def actionType: String

    def insights: String

    def taskPath: Array[Int]

}

sealed abstract class AbstractStep(val actionType: String) extends Step {
    override val taskPath = Thread.currentThread() match {
        case w: Worker => w.getTaskStack
        case _         => Array()
    }
}

case class RequestStep(requestType: String, goal: String, targetEngine: String, channel: TrafficReference) extends AbstractStep("request") {
    override def insights: String = s"Performing request '$requestType' in channel $channel with goal: $goal. waiting $targetEngine to answer..."
}

case class ResponseStep(requestType: String, senderEngine: String, channel: TrafficReference) extends AbstractStep("response") {
    override def insights: String = s"Performing response to request '$requestType' in channel $channel. engine '$senderEngine' is currently waiting answer..."
}

case class MethodInvocationSendStep(agreement: RMIRulesAgreement, method: MethodDescription, appointedEngine: String) extends AbstractStep("rmi - send") {
    override def insights: String = s"Performing remote method invocation following agreement $agreement, for method $method." + {
        if (appointedEngine == null) "" else s" waiting for engine '$appointedEngine' to return or throw."
    }
}

case class MethodInvocationComputeStep(methodID: Int, triggerEngine: String, isWaiting: Boolean) extends AbstractStep("rmi - compute") {
    override def insights: String = s"Computing method invocation (id $methodID) triggered by $triggerEngine." + {
        if (!isWaiting) "" else s" remote is waiting this method invocation to return or throw."
    }
}

case class MethodInvocationExecutionStep(method: MethodDescription, triggerEngine: String, isWaiting: Boolean) extends AbstractStep("rmi - execute") {
    override def insights: String = s"Executing method $method triggered by $triggerEngine." + {
        if (!isWaiting) "" else s" remote is waiting this method to return or throw."
    }
}

case class PacketInjectionStep(packetID: String, channel: TrafficReference) extends AbstractStep("injection") {
    override def insights: String = s"Injecting packet $packetID into channel $channel."
}

case class TaskPausedStep(taskID: Int, timeout: Long = 0) extends AbstractStep("task pause") {
    override def insights: String = s"Task $taskID is paused." + (if (timeout > 0) " timeout = " + timeout else "")
}

case class SIPURectifyStep(channel: TrafficReference, currentOrdinal: Int, expectedOrdinal: Int) extends AbstractStep("SIPU - rectify") {
    override def insights: String = {
        val diff = currentOrdinal - expectedOrdinal
        s"Waiting remaining packets for channel $channel. SIPU's Head of queue ordinal is $diff ahead expected ordinal of $expectedOrdinal."
    }
}

case class PacketSerializationStep(packetID: String, target: String) extends AbstractStep("serialize") {
    override def insights: String = s"Serializing packet '$packetID' for targeted engine $target."
}

case class PacketDeserializationStep(packetID: String, sender: String) extends AbstractStep("deserialize") {
    override def insights: String = s"Deserializing packet '$packetID' sent from engine $sender."
}

case class ConnectedObjectTreeRetrievalStep(reference: ConnectedObjectReference) extends AbstractStep("network object retrieval") {
    override def insights: String = s"Retrieving network object tree $reference..."
}

case class ConnectedObjectCreationStep(objectReference: ConnectedObjectReference) extends AbstractStep("connected object creation") {
    override def insights: String = s"Creating connected object $objectReference."
}

case class CacheOpenStep(cacheReference: SharedCacheReference, cacheType: Class[_ <: SharedCache]) extends AbstractStep("cache open") {
    override def insights: String = s"opening cache of type '${cacheType.getName}' at $cacheReference."
}