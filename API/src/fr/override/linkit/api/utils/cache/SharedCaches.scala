package fr.`override`.linkit.api.utils.cache

import fr.`override`.linkit.api.packet.collector.AsyncPacketCollector
import fr.`override`.linkit.api.packet.traffic.PacketTraffic

import scala.collection.mutable

object SharedCaches {

    def synchronise[T](t: T, channelID: Int)(implicit traffic: PacketTraffic): SharedInstance[T] = {
        val sharedInstance = new SharedInstance[T](genCollector(traffic, channelID))
        sharedInstance.set(t)
        sharedInstance
    }

    private def genCollector(traffic: PacketTraffic, channelID: Int): AsyncPacketCollector = {
        traffic.createCollector(channelID, AsyncPacketCollector)
    }

    def synchronise[T](collection: mutable.Seq[T], channelID: Int)(implicit traffic: PacketTraffic): SharedCollection[T] = {
        val sharedInstance = new SharedCollection[T](genCollector(traffic, channelID))
        collection.foreach(sharedInstance.add)
        sharedInstance
    }

    def createCollection[A](channelID: Int, typeOfT: Class[A] = null)(implicit traffic: PacketTraffic): SharedCollection[A] = {
        new SharedCollection[A](traffic.createCollector(channelID, AsyncPacketCollector))
    }

    def createInstance[A](channelID: Int, typeOfT: Class[A] = null)(implicit traffic: PacketTraffic): SharedInstance[A] = {
        new SharedInstance[A](traffic.createCollector(channelID, AsyncPacketCollector))
    }

    def createInstance[A, B](channelID: Int, mapper: A => B, typeOfa: Class[A] = null)(implicit traffic: PacketTraffic): SharedInstance[B] = {
        new SharedInstance[B](traffic.createCollector(channelID, AsyncPacketCollector))
    }

}
