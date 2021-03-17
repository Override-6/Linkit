package fr.`override`.linkit.core.internal.system.fsa

import fr.`override`.linkit.skull.internal.system.fsa.io.IOFileSystemAdapter
import fr.`override`.linkit.skull.internal.system.fsa.nio.NIOFileSystemAdapter

object JDKFileSystemAdapters {

    lazy val Nio = new NIOFileSystemAdapter
    lazy val Io = new IOFileSystemAdapter

}
