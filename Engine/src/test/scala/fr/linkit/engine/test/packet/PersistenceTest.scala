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

package fr.linkit.engine.test.packet

import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.DedicatedPacketCoordinates
import fr.linkit.api.gnom.persistence.PersistenceBundle
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel
import fr.linkit.engine.gnom.cache.sync.generation.sync.{DefaultSyncClassCenter, SyncClassStorageResource}
import fr.linkit.engine.gnom.persistence.config.PersistenceConfigBuilder
import fr.linkit.engine.gnom.persistence.serializor.DefaultObjectPersistence
import fr.linkit.engine.test.mocks.ContextObjectLinkerMock
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Test, TestInstance}
import org.mockito.Mockito.mock

import java.nio.ByteBuffer

@TestInstance(Lifecycle.PER_CLASS)
class PersistenceTest {
    
    import fr.linkit.engine.application.resource.external.LocalResourceFolder._
    
    private val resources       = TestEngine.resources.getOrOpenThenRepresent[SyncClassStorageResource]("SyncClasses")
    private val syncClassCenter = new DefaultSyncClassCenter(TestEngine.classCenter, resources)
    private val persistor       = new DefaultObjectPersistence(syncClassCenter)
    
    private val testBuffer    = ByteBuffer.allocate(150000)
    private val minimalConfig = PersistenceConfigBuilder
            .fromScript(getClass.getResource("/default_scripts/persistence_minimal.sc"), TestEngine.traffic)
            .build(new ContextObjectLinkerMock(true))
    
    @Test
    def persistPacket(): Unit = {
        val coords = DedicatedPacketCoordinates(Array(), "target", "sender")
        persistor.serializeObjects(Array(Map("A" -> 'a', "B" -> 'b')))(new PersistenceBundle {
            override val network   : Network           = TestEngine.network
            override val buff      : ByteBuffer        = testBuffer
            override val boundId   : String            = coords.senderID
            override val packetPath: Array[Int]        = coords.path
            override val config    : PersistenceConfig = minimalConfig
        })
    }
    
}
