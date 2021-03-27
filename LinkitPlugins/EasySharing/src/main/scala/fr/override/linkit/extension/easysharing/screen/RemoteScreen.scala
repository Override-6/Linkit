package fr.`override`.linkit.extension.easysharing.screen

class RemoteScreen(network: Network) extends RemoteFragment {
    override val nameIdentifier: String = "RemoteFragment"

    private val selfEntity = network.selfEntity
    private val ownCache = selfEntity.cache
    private val relayID = selfEntity.identifier
    private val viewers = ownCache.get(18, SharedCollection[String])
    private val spectatingScreens = mutable.HashMap.empty[String, RemoteScreenViewer]

    override def handleRequest(packet: Packet, coords: DedicatedPacketCoordinates): Unit = {
        packet match {
            case StreamPacket(frameBuff) =>
                spectatingScreens.get(coords.senderID).foreach(_.pushFrame(frameBuff))
        }
    }

    override def start(): Unit = {

    }

    override def destroy(): Unit = {
        packetSender().close()
    }

    def spectate(target: String): RemoteScreenViewer = {
        val targetCache = network.getEntity(target).get.cache
        spectatingScreens.getOrElseUpdate(target, {
            targetCache.get(18, SharedCollection[String])
                    .add(relayID)
            new RemoteScreenViewer()
        })
    }

    def isSpectating(target: String): Boolean = spectatingScreens.contains(target)

    def stopSpectating(target: String): Unit = {
        val targetCache = network.getEntity(target).get.cache
        targetCache.get(18, SharedCollection[String])
                .remove(relayID)
        spectatingScreens.remove(target)
    }

    def isViewing(target: String): Boolean = viewers.contains(target)

    /*private def startScreenRecorder(): Unit = Application.run(() => {
        val region = new Rectangle2D(0, 0, 1920, 1080)
        //val robot = new Robot()
        val writable = new WritableImage(1920, 1080)
        val reader = writable.getPixelReader
        val buffer = new Array[Int](1920 * 1080)
        viewers.addListener((_, _, _) => viewers.synchronized {
            viewers.notifyAll()
        })

        while (true) {
            while (viewers.nonEmpty) {
                //robot.getScreenCapture(writable, region)
                println("Capture created !")
                reader.getPixels(0, 0, 1920, 1080, PixelFormat.getIntArgbInstance, buffer, 0, region.getWidth.toInt)
                packetSender().sendTo(StreamPacket(buffer), viewers.toSeq: _*)
                println("Bytes sent !!")
            }
            println("NO VIEWERS LEFT.")
            viewers.synchronized {
                println("WAITING FOR VIEWERS")
                viewers.wait()
                println("STREAM STARTED AGAIN.")
            }
        }
    })

    startScreenRecorder()

*/

}

object RemoteScreen {
    private case class StreamPacket(stream: Array[Int]) extends Packet
}
