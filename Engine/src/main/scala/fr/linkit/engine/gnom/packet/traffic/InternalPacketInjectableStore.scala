package fr.linkit.engine.gnom.packet.traffic

import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.packet.traffic.injection.PacketInjectionController

trait InternalPacketInjectableStore {

    def getPersistenceConfig(path: Array[Int]): PersistenceConfig

    protected def getPersistenceConfig(path: Array[Int], pos: Int): PersistenceConfig

    def inject(injection: PacketInjectionController): Unit
}
