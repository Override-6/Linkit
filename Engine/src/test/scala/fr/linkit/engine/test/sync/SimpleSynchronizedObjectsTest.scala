/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.test.sync

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.gnom.cache.sync.SynchronizedObjectCache
import fr.linkit.api.gnom.cache.sync.behavior.annotation.BasicInvocationRule
import fr.linkit.api.gnom.packet.Packet
import fr.linkit.api.test.HierarchyRaiserOrderer
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache
import fr.linkit.engine.gnom.cache.sync.behavior.{AnnotationBasedMemberBehaviorFactory, ObjectBehaviorBuilder, ObjectBehaviorStoreBuilder}
import fr.linkit.engine.gnom.cache.sync.behavior.ObjectBehaviorBuilder.{MethodControl, ParameterControl}
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.gnom.packet.traffic.channel.SyncPacketChannel
import fr.linkit.engine.test.Player
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{TestInstance, TestMethodOrder}

import scala.collection.mutable.ListBuffer

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(classOf[HierarchyRaiserOrderer])
abstract class SimpleSynchronizedObjectsTest {

    protected def app: ApplicationContext

    protected val connection: ConnectionContext                           = app.findConnection(48484).get
    private   val testChannel                                             = {
        connection.traffic
                .getInjectable[SyncPacketChannel](500, ChannelScopes.discardCurrent)
    }

    private val tree = new ObjectBehaviorStoreBuilder(AnnotationBasedMemberBehaviorFactory) {
        behaviors += new ObjectBehaviorBuilder[ListBuffer[Player]]() {
            annotateAllMethods("+=") and "addOne" by new MethodControl(BasicInvocationRule.BROADCAST) {
                arg(0)(new ParameterControl[Player](true))
            }
        }
    }.build
    protected val testCache : SynchronizedObjectCache[ListBuffer[Player]] = connection.network.globalCache.attachToCache(51, DefaultSynchronizedObjectCache[ListBuffer[Player]](tree))

    protected def unlockNextStep(): Unit = testChannel.send(EmptyPacket)

    protected def waitNextStep(): Unit = testChannel.nextPacket[Packet]

}

object SimpleSynchronizedObjectsTest {

    final val OverrideMotherPlayer = Player(78, "Override's mom", "Override", x = 100, y = 150)

}
