package fr.`override`.linkit.api.`extension`.fragment

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.`extension`.{LoadPhase, RelayExtension, RelayExtensionLoader}
import fr.`override`.linkit.api.exception.ExtensionLoadException
import fr.`override`.linkit.api.packet.channel.{AsyncPacketChannel, PacketChannel, SyncPacketChannel}
import fr.`override`.linkit.api.packet.collector.{AsyncPacketCollector, SyncPacketCollector}
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.utils.WrappedPacket

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class FragmentHandler(relay: Relay, extensionLoader: RelayExtensionLoader) {

    private val fragmentMap: mutable.Map[Class[_ <: RelayExtension], ExtensionFragments] = mutable.Map.empty

    private val remoteFragmentRequestCollector = relay.createCollector(PacketTraffic.RemoteFragmentsReqCollectorID, AsyncPacketCollector)
    private val remoteFragmentResponseCollector = relay.createCollector(PacketTraffic.RemoteFragmentsRespCollectorID, SyncPacketCollector)

    def setFragment(fragment: ExtensionFragment)(implicit extension: RelayExtension): Unit = {
        if (extensionLoader.getPhase != LoadPhase.LOAD)
            throw new IllegalStateException("Could not set fragment : fragmentMap can only be set during LOAD phase")

        val extensionClass = extension.getClass
        val fragmentClass = fragment.getClass
        if (getFragment(extensionClass, fragmentClass).isDefined)
            throw new IllegalArgumentException("This fragment kind is already set for this extension")

        fragmentMap.getOrElseUpdate(extensionClass, new ExtensionFragments)
                .setFragment(fragment)

    }

    def getFragment[F <: ExtensionFragment](extensionClass: Class[_ <: RelayExtension], fragmentClass: Class[F]): Option[F] = {
        val fragmentsOpt = fragmentMap.get(extensionClass)
        if (fragmentsOpt.isEmpty)
            return None

        fragmentsOpt
                .get
                .getFragment(fragmentClass)
    }

    def listRemoteFragments(): List[RemoteFragment] = {
        val fragments = ListBuffer.empty[ExtensionFragment]
        fragmentMap.values
                .foreach(_.list()
                        .foreach(fragments.addOne))
        fragments.filter(_.isInstanceOf[RemoteFragment])
                .map(_.asInstanceOf[RemoteFragment])
                .toList
    }

    def getRemoteFragmentsChannel(targetID: String): PacketChannel.Async = {
        remoteFragmentRequestCollector.subChannel(targetID, AsyncPacketChannel)
    }

    private[extension] def startFragments(): Int = {
        var count = 0
        fragmentMap.values.foreach(fragments => {
            count += fragments.startAll()
        })
        count
    }

    private[extension] def startFragments(extensionClass: Class[_ <: RelayExtension]): Unit = {
        fragmentMap.get(extensionClass).foreach(_.startAll())
    }

    private[extension] def destroyFragments(): Unit = {
        fragmentMap.values.foreach(_.destroyAll())
    }

    remoteFragmentRequestCollector.addOnPacketInjected((pack, coords) => {
        pack match {
            case fragmentPacket: WrappedPacket =>
                val fragmentName = fragmentPacket.category
                val packet = fragmentPacket.subPacket
                val sender = coords.senderID
                val responseChannel = remoteFragmentResponseCollector.subChannel(sender, SyncPacketChannel)
                listRemoteFragments()
                        .find(_.nameIdentifier == fragmentName)
                        .foreach(fragment => fragment.handleRequest(packet, responseChannel))
        }
    })

    private class ExtensionFragments {
        private val fragments: mutable.Map[Class[_ <: ExtensionFragment], ExtensionFragment] = mutable.Map.empty

        def getFragment[F <: ExtensionFragment](fragmentClass: Class[F]): Option[F] = {
            fragments.get(fragmentClass).asInstanceOf[Option[F]]
        }

        def setFragment(fragment: ExtensionFragment): Unit = {
            fragments.put(fragment.getClass, fragment)
        }

        def startAll(): Int = {
            for (fragment <- Map.from(fragments).values) {
                try {
                    fragment.start()
                } catch {
                    case NonFatal(e) =>
                        fragments.remove(fragment.getClass)
                        throw ExtensionLoadException(s"Could not start fragment : Exception thrown while starting it", e)
                }
            }
            //Notifying the network that some remote fragments where added.
            val names = fragments.values
                    .filter(_.isInstanceOf[RemoteFragment])
                    .map(_.asInstanceOf[RemoteFragment].nameIdentifier)
                    .toArray
            if (names.length > 0) {
                //network.notifyLocalRemoteFragmentSet(names)
            }

            fragments.size
        }

        /**
         * Fragments must be destroyed only once the relay is closed.
         * */
        def destroyAll(): Unit = {
            fragments.values.foreach(_.destroy())
        }

        def list(): Iterable[ExtensionFragment] = {
            fragments.values
        }

    }

}