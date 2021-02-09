package fr.`override`.linkit.api.packet

trait PacketCompanion[P <: Packet] {
    val identifier: Int

    def companion: this.type = this //for Java Users
}
