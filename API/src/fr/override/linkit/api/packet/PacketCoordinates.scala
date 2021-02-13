package fr.`override`.linkit.api.packet

import fr.`override`.linkit.api.packet.serialization.PacketSerializer

trait PacketCoordinates {

    def determineSerializer(array: Array[String], raw: PacketSerializer, cached: PacketSerializer): PacketSerializer

}