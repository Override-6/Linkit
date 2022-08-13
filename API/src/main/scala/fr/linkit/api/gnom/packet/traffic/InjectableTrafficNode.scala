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

package fr.linkit.api.gnom.packet.traffic

import fr.linkit.api.gnom.packet.traffic.unit.InjectionProcessorUnit
import fr.linkit.api.gnom.persistence.obj.{TrafficObjectReference, TrafficReference}

trait InjectableTrafficNode[+C <: PacketInjectable] extends TrafficNode[C] {
    
    /**
     * Sets the injection method for this injectable to "performant".
     * Performant injection will perform packet injection in any order and asynchronously.
     * <p>
     *     If the injectable is used to initialize something, the "performant" state should be avoided.
     *     Keep in mind that the persistence system is mutable.
     *     Thus, if any packet deserialization / serialization action is able to perform modifications on the GNOM set of an engine,
     *     the performant injection should also be avoided.
     *     However, for anything else that just needs speed (streaming etc), the performance state can be interesting.
     * </p>
     * <p>
     *     __Note__ : if [[persistenceConfig.autoContextObjects]] is enabled while using this mode, the persistence system could
     *     end up with [[fr.linkit.api.gnom.persistence.context.ContextualObjectReference]] that are badly synchronized with other engines.
     *     This could have for effect, depending on the amount of object that are to resynchronize,
     *     to use much more bandwidth because the persistence system will be forced to send
     *     a lot of packets in order to retrieve/send objects that are referenced by ContextualObjectReferences, but that could not point to an actual object
     * </p>
     * */
    def setPerformantInjection(): this.type
    
    /**
     * Sets the injection method for this injectable to "sequential".
     * Sequential injection will perform packet injection in one thread,
     * and ensure that the packets get deserialized and injected in the right order.
     * <p>
     *     Should be used during initialization of anything, or if the packet discussion is able to produce boarding effect on the engines.
     *
     * </p>
     * */
    def setSequentialInjection(): this.type
    
    /**
     * @return true if the current injection method is set to Performant
     * */
    def preferPerformances(): Boolean
    
    def chainIPU(reference: TrafficObjectReference): this.type = chainTo(reference.trafficPath)
    
    def chainIPU(trafficNode: TrafficNode[TrafficObject[TrafficReference]]): Unit = {
        chainTo(trafficNode.injectable.trafficPath)
    }
    
    def chainTo(path: Array[Int]): this.type
    
    /**
     * Returns the used [[InjectionProcessorUnit]].
     * The InjectionProcessorUnit (or IPU) is used by the PacketTraffic to inject and trigger deserialization of a packet.
     * */
    def unit(): InjectionProcessorUnit
    
}

object InjectableTrafficNode {
    
    implicit def unwrap[C <: PacketInjectable](node: TrafficNode[C]): C = node.injectable
}
