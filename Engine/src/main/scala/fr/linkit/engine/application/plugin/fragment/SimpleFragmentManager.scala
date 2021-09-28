/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.application.plugin.fragment

import fr.linkit.api.application.plugin.fragment.{FragmentManager, PluginFragment, RemoteFragment}
import fr.linkit.api.application.plugin.{LinkitPlugin, Plugin, PluginLoadException}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class SimpleFragmentManager() extends FragmentManager {

    private val fragmentMap: mutable.Map[Class[_ <: Plugin], PluginFragments] = mutable.Map.empty

    override def putFragment(fragment: PluginFragment)(implicit owner: Plugin): Unit = {
        val pluginClass   = owner.getClass
        val fragmentClass = fragment.getClass
        if (getFragment(pluginClass, fragmentClass).isDefined)
            throw new IllegalArgumentException("This fragment kind is already set for this extension")

        fragmentMap.getOrElseUpdate(pluginClass, new PluginFragments)
                .putFragment(fragment)

        fragment match {
            case remote: RemoteFragment =>
            //TODO initRemote(remote)

            case _ =>
        }

    }

    override def getFragment[F <: PluginFragment](extensionClass: Class[_ <: Plugin], fragmentClass: Class[F]): Option[F] = {
        val fragmentsOpt = fragmentMap.get(extensionClass)
        if (fragmentsOpt.isEmpty)
            return None

        fragmentsOpt
                .get
                .getFragment(fragmentClass)
    }

    def listRemoteFragments(): List[RemoteFragment] = {
        val fragments = ListBuffer.empty[PluginFragment]
        fragmentMap.values
                .foreach(_.list()
                        .foreach(fragments.addOne))
        fragments.filter(_.isInstanceOf[RemoteFragment])
                .map(_.asInstanceOf[RemoteFragment])
                .toList
    }

    private[plugin] def startFragments(): Int = {
        var count = 0
        fragmentMap.values.foreach(fragments => {
            count += fragments.startAll()
        })
        count
    }

    private[plugin] def startAllFragments(extensionClass: Class[_ <: LinkitPlugin]): Unit = {
        fragmentMap.get(extensionClass).foreach(_.startAll())
    }

    private[plugin] def destroyFragments(extensionClass: Class[_ <: Plugin]): Unit = {
        fragmentMap.get(extensionClass).foreach(_.destroyAll())
        fragmentMap.remove(extensionClass)
    }

    private[plugin] def destroyAllFragments(): Unit = {
        fragmentMap.values.foreach(_.destroyAll())
        fragmentMap.clear()
    }

    private class PluginFragments {

        private val fragments: mutable.Map[Class[_ <: PluginFragment], PluginFragment] = mutable.Map.empty

        def getFragment[F <: PluginFragment](fragmentClass: Class[F]): Option[F] = {
            fragments.get(fragmentClass).asInstanceOf[Option[F]]
        }

        def putFragment(fragment: PluginFragment): Unit = {
            fragments.put(fragment.getClass, fragment)
        }

        def startAll(): Int = {
            for (fragment <- Map.from(fragments).values) {
                try {
                    fragment.start()
                } catch {
                    case NonFatal(e) =>
                        fragments.remove(fragment.getClass)
                        throw PluginLoadException(s"Could not start fragment : Exception thrown while starting it", e)
                }
            }
            //Notifying the network that some remote fragments where added.
            val names = fragments.values
                    .filter(_.isInstanceOf[LinkitRemoteFragment])
                    .map(_.asInstanceOf[LinkitRemoteFragment].nameIdentifier)
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
            fragments.clear()
        }

        def list(): Iterable[PluginFragment] = {
            fragments.values
        }

    }

}