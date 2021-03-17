package fr.`override`.linkit.skull.connection.packet.traffic

trait PacketInjectableFactory[C <: PacketInjectable] {

    def createNew(scope: ChannelScope): C

}
