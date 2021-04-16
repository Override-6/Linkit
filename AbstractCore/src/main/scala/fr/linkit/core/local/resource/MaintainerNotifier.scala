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

package fr.linkit.core.local.resource

import fr.linkit.api.local.resource.{ResourcesMaintainer, ResourcesMaintainerInformer}
import fr.linkit.api.local.system.fsa.FileSystemAdapter

import java.nio.file._
import java.util
import scala.collection.mutable

class MaintainerNotifier(fsa: FileSystemAdapter, path: String) {

    private val watcher: WatchService = FileSystems.getDefault.newWatchService()
    private val maintainers           = new mutable.HashMap[String, ResourcesMaintainer with ResourcesMaintainerInformer]()

    @volatile private var alive = true

    def startWatchService(): Unit = {
        if (alive)
            throw new IllegalStateException("This Resource folder event listener is already alive !")
        new Thread(() => {
            while (alive) {
                val key    = watcher.poll()
                val events = key.pollEvents().asInstanceOf[util.List[WatchEvent[Path]]]
                events.forEach(event => {
                    val path = event.context().toAbsolutePath
                    val folder = path.getParent
                    maintainers.get(folder.toString).fold() { maintainer =>
                        maintainer.informLocalModification(path.getFileName.toString)
                    }
                })
                key.reset()
            }
        }, "Resources Maintainers Listener").start()
        alive = true
    }

    def close(): Unit = {
        alive = false
        watcher.close()
    }

    def addMaintainer(maintainer: ResourcesMaintainer with ResourcesMaintainerInformer): Unit = {
        val location = maintainer.getResources.getLocation
        maintainers.put(location, maintainer)
        val path = Path.of(location)
        import StandardWatchEventKinds._
        path.register(watcher, ENTRY_MODIFY)
    }

}
