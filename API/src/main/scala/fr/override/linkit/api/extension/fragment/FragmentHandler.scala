/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.api.extension.fragment

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.ExtensionLoadException
import fr.`override`.linkit.api.extension.{LoadPhase, RelayExtension, RelayExtensionLoader}
import fr.`override`.linkit.api.network.cache.collection.SharedCollection
import fr.`override`.linkit.api.packet.traffic.ChannelScope
import fr.`override`.linkit.api.packet.traffic.channel.{AsyncPacketChannel, PacketChannelCategories}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class FragmentHandler(relay: Relay, extensionLoader: RelayExtensionLoader) {

    private val fragmentMap: mutable.Map[Class[_ <: RelayExtension], ExtensionFragments] = mutable.Map.empty

    private val categories = relay.getInjectable(4, ChannelScope.broadcast, PacketChannelCategories)

    private lazy val sharedRemoteFragments: SharedCollection[String] = {
        var ptn: SharedCollection[String] = null
        ptn = relay.network
                .selfEntity
                .cache
                .get(6, SharedCollection.set[String])

        ptn
    }


    def initRemote(remote: RemoteFragment): Unit = {
        sharedRemoteFragments.add(remote.nameIdentifier)
        val channel = categories.createCategory(remote.nameIdentifier, ChannelScope.broadcast, AsyncPacketChannel)
        remote.setChannel(channel)
    }

    def putFragment(fragment: ExtensionFragment)(implicit extension: RelayExtension): Unit = {
        if (extensionLoader.getPhase != LoadPhase.LOADING)
            throw new IllegalStateException("Could not set fragment : fragment can only be put during LOAD phase")

        val extensionClass = extension.getClass
        val fragmentClass = fragment.getClass
        if (getFragment(extensionClass, fragmentClass).isDefined)
            throw new IllegalArgumentException("This fragment kind is already set for this extension")

        fragmentMap.getOrElseUpdate(extensionClass, new ExtensionFragments)
                .putFragment(fragment)

        fragment match {
            case remote: RemoteFragment =>
                initRemote(remote)

            case _ =>
        }

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

    private class ExtensionFragments {
        private val fragments: mutable.Map[Class[_ <: ExtensionFragment], ExtensionFragment] = mutable.Map.empty

        def getFragment[F <: ExtensionFragment](fragmentClass: Class[F]): Option[F] = {
            fragments.get(fragmentClass).asInstanceOf[Option[F]]
        }

        def putFragment(fragment: ExtensionFragment): Unit = {
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