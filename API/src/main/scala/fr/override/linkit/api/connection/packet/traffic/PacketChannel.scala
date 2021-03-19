package fr.`override`.linkit.api.connection.packet.traffic

import fr.`override`.linkit.api.local.system.JustifiedCloseable


trait PacketChannel extends JustifiedCloseable {

    val ownerID: String
    val traffic: PacketTraffic
    val identifier: Int

}
