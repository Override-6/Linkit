package fr.linkit.engine.application.packet.traffic

import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.application.packet.traffic.injection.PacketInjectionController

trait InternalPacketInjectableStore {

    def getPersistenceConfig(path: Array[Int]): PersistenceConfig
//
    protected def getPersistenceConfig(path: Array[Int], pos: Int): PersistenceConfig

    def inject(injection: PacketInjectionController): Unit
}
