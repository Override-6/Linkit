package fr.`override`.linkit.api.packet.traffic

trait PacketInjectableFactory[C <: PacketInjectable] {

    def createNew(scope: ChannelScope): C

}
