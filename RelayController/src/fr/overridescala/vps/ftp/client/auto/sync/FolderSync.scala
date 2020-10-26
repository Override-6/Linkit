package fr.overridescala.vps.ftp.client.auto.sync

import java.io.File
import java.nio.file._
import java.util.concurrent.ThreadLocalRandom

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.DataPacket
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}
import fr.overridescala.vps.ftp.api.utils.Utils
import fr.overridescala.vps.ftp.client.auto.Automation

import scala.collection.mutable.ListBuffer

class FolderSync(relay: Relay,
                 targetRelay: String,
                 localPath: String,
                 targetPath: String,
                 channelID: Int) extends Automation with PathEventListener {

    implicit private val channel: PacketChannel = relay.createChannel(targetRelay, channelID)
    private val ignoredPaths = ListBuffer.empty[String]
    private val random = ThreadLocalRandom.current()
    private val folderWatcher = new FolderWatcher(Path.of(localPath))
    folderWatcher.register(this)

    channel.setOnPacketAdded(p => {
        handlePacket(p)
        false
    })

    override def start(): Unit = folderWatcher.start(1500)

    override def onCreate(affected: Path): Unit = onModify(affected)

    override def onDelete(affected: Path): Unit = channel.sendPacket(DataPacket("remove", affected.toString))

    override def onModify(affected: Path): Unit = {
        if (ignoredPaths.contains(affected.toString))
            return
        val in = Files.newInputStream(affected)
        channel.sendPacket(DataPacket(s"SUPLOAD:$affected", in.readAllBytes()))
        in.close()
    }

    override def onRename(affected: Path, newName: String): Unit = {
        if (ignoredPaths.contains(affected.toString))
            return
        channel.sendPacket(DataPacket(s"rename to $newName", affected.toString))
    }

    private def deletePath(path: Path): Unit = {
        if (Files.notExists(path))
            return
        deleteRecursively(path)

        def deleteRecursively(path: Path): Unit = {
            if (Files.notExists(path))
                return
            if (!Files.isDirectory(path)) {
                Files.delete(path)
            } else {
                Files.list(path).forEach(deleteRecursively)
                Files.move(path, path.resolveSibling(path.getFileName + s"(added to remove list) #${random.nextInt(0, Short.MaxValue)}"))
            }
        }
    }

    private def handlePacket(packet: Packet): Unit = {
        val data = packet.asInstanceOf[DataPacket]
        val order = data.header
        if (order.contains(':')) {
            handleComplexOrder(data)
            return
        }
        val path = toLocal(new String(data.content))

        if (order.startsWith("rename to") && Files.exists(path)) {
            val newName = order.split(" ").last
            val renamed = path.resolveSibling(newName)
            ignoredPaths += renamed.toString
            Files.move(path, renamed, StandardCopyOption.REPLACE_EXISTING)
            return
        }

        ignoredPaths += path.toString
        order match {
            case "delete" => deletePath(path)
            case "mkdirs" if Files.notExists(path) => Files.createDirectories(path)
            case _ =>
        }
    }

    private def handleComplexOrder(data: DataPacket): Unit = {
        val order = data.header
        val affectedPath = order.split(":").last
        val path = toLocal(affectedPath)
        ignoredPaths += affectedPath

        if (order.startsWith("SUPLOAD")) {
            val out = Files.newOutputStream(path, StandardOpenOption.CREATE)
            out.write(data.content)
            out.flush()
            out.close()
        }
    }

    private def toLocal(nonLocalPath: String): Path = {
        val relativePath = Utils.subPathOfUnknownFile(nonLocalPath, targetNameCount)
        Utils.formatPath(localPath + relativePath)
    }

    private val targetNameCount = targetPath.count(_ == File.separatorChar)

}
