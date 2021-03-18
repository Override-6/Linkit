package fr.`override`.linkit.api.connection.packet.traffic

trait PacketInjectableFactory[C <: PacketInjectable] {

    def createNew(scope: ChannelScope): C

}
