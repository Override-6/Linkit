package fr.`override`.linkit.api.local.system.config

import fr.`override`.linkit.api.local.system.fsa.FileSystemAdapter

trait LinkitApplicationConfiguration {

    val extensionsFolder: String //can be relative or global
    val enableEventHandling: Boolean
    val nWorkerThreadFunction: Int => Int
    val degressiveThreadPool: Boolean

    val fsAdapter: FileSystemAdapter
    val checker: Checker

}
