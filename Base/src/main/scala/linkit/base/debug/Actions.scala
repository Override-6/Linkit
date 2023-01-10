/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package linkit.base.debug

import fr.linkit.api.gnom.cache.sync.ConnectedObjectReference
import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIDispatchAgreement
import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.{SharedCache, SharedCacheReference}
import fr.linkit.api.gnom.network.tag.{EngineTag, UniqueTag}
import fr.linkit.api.gnom.persistence.obj.TrafficReference
import fr.linkit.api.internal.concurrency.Procrastinator

sealed trait Step {
    def actionType: String

    def insights: String

    def taskID: Int

}

sealed abstract class AbstractStep(val actionType: String) extends Step {
    override val taskID: Int = Procrastinator.currentWorker.map(_.taskID).getOrElse(-1)
}

case class RequestStep(requestType: String, goal: String, target: EngineTag, channel: TrafficReference) extends AbstractStep("request") {
    override def insights: String = s"Performing request '$requestType' in channel $channel with goal: $goal. waiting $target to answer..."
}

case class ResponseStep(requestType: String, senderEngine: UniqueTag, channel: TrafficReference) extends AbstractStep("response") {
    override def insights: String = s"Performing response to request '$requestType' in channel $channel. engine '$senderEngine' is currently waiting answer..."
}

case class MethodInvocationSendStep(agreement: RMIDispatchAgreement, method: MethodDescription, appointedEngine: UniqueTag) extends AbstractStep("rmi - send") {
    override def insights: String = s"Performing remote method invocation following agreement $agreement, for method $method." + {
        if (appointedEngine == null) "" else s" waiting for engine '$appointedEngine' to return or throw."
    }
}

case class MethodInvocationComputeStep(methodID: Int, triggerEngine: UniqueTag, isWaiting: Boolean) extends AbstractStep("rmi - compute") {
    override def insights: String = s"Computing method invocation (id $methodID) triggered by $triggerEngine." + {
        if (!isWaiting) "" else s" remote is waiting this method invocation to return or throw."
    }
}

case class MethodInvocationExecutionStep(method: MethodDescription, triggerEngine: String) extends AbstractStep("rmi - execute") {
    override def insights: String = s"Executing method $method triggered by $triggerEngine." + {
        "" //if (!isWaiting) "" else s" remote is waiting this method to return or throw."
    }
}

case class PacketInjectionStep(packetID: String, channel: TrafficReference) extends AbstractStep("injection") {
    override def insights: String = s"Injecting packet $packetID into channel $channel."
}

case class SIPURectifyStep(channel: TrafficReference, received: Int, expectedOrdinal: Int) extends AbstractStep("SIPU - rectify") {
    override def insights: String = {
        val diff = received - expectedOrdinal
        s"Waiting remaining packets for channel $channel. SIPU's Head of queue ordinal is $diff ahead expected ordinal of $expectedOrdinal."
    }
}

case class PacketSerializationStep(packetID: String, target: UniqueTag) extends AbstractStep("serialize") {
    override def insights: String = s"Serializing packet '$packetID' for targeted engine $target."
}

case class PacketDeserializationStep(packetID: String, sender: UniqueTag) extends AbstractStep("deserialize") {
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