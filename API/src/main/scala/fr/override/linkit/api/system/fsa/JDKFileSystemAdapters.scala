package fr.`override`.linkit.api.system.fsa

import fr.`override`.linkit.api.system.fsa.io.IOFileSystemAdapter
import fr.`override`.linkit.api.system.fsa.nio.NIOFileSystemAdapter

object JDKFileSystemAdapters {

    lazy val Nio = new NIOFileSystemAdapter
    lazy val Io = new IOFileSystemAdapter

}
