package fr.`override`.linkit.api.concurrency

import scala.annotation.StaticAnnotation

/**
 * Specifies that this method or constructor will be executed by a packet worker thread
 * @see [[PacketWorkerThread]]
 * */
class packetWorkerExecution extends StaticAnnotation {

}
