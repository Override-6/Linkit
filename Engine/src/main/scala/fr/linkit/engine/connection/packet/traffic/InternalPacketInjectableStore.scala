package fr.linkit.engine.connection.packet.traffic

import fr.linkit.api.connection.packet.persistence.context.PersistenceConfig
import fr.linkit.api.connection.packet.traffic.injection.PacketInjectionController

trait InternalPacketInjectableStore {

    def getPersistenceConfig(path: Array[Int]): PersistenceConfig

    protected def getPersistenceConfig(path: Array[Int], pos: Int): PersistenceConfig

    def inject(injection: PacketInjectionController): Unit
}
