package fr.`override`.linkit.api.utils.cache

import fr.`override`.linkit.api.packet.collector.CommunicationPacketCollector
import fr.`override`.linkit.api.packet.traffic.PacketTraffic

import scala.collection.mutable

object SharedCaches {

    def synchronise[A <: Serializable](t: A, channelID: Int)(implicit traffic: PacketTraffic): SharedInstance[A] = {
        val sharedInstance = new SharedInstance[A](genCollector(traffic, channelID))
        sharedInstance.set(t)
        sharedInstance
    }

    private def genCollector(traffic: PacketTraffic, channelID: Int): CommunicationPacketCollector = {
        traffic.openCollector(channelID, CommunicationPacketCollector)
    }

    def synchronise[A <: Serializable](collection: mutable.Seq[A], channelID: Int)(implicit traffic: PacketTraffic): SharedCollection[A] = {
        val sharedInstance = SharedCollection.open[A](channelID)
        collection.foreach(sharedInstance.add)
        sharedInstance
    }

    def retrieveInstance[A <: Serializable](channelID: Int)(implicit traffic: PacketTraffic): A = {
        val shared = sharedInstance(channelID)
        val instance: A = shared.get
        shared.close()
        instance
    }

    def sharedInstance[A <: Serializable](channelID: Int)(implicit traffic: PacketTraffic): SharedInstance[A] = {
        new SharedInstance[A](traffic.openCollector(channelID, CommunicationPacketCollector))
    }

}
