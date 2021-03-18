package fr.`override`.linkit.core.local.system.fsa

import fr.`override`.linkit.api.local.system.fsa.io.IOFileSystemAdapter
import fr.`override`.linkit.api.local.system.fsa.nio.NIOFileSystemAdapter

object JDKFileSystemAdapters {

    lazy val Nio = new NIOFileSystemAdapter
    lazy val Io = new IOFileSystemAdapter

}
