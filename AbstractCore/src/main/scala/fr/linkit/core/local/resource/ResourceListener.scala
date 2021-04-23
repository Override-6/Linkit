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

import fr.linkit.api.local.resource.{ResourceKey, ResourcesMaintainer}

import java.nio.file._
import java.util
import scala.collection.mutable

class ResourceListener(resourcePath: String) {

    private val rootPath              = Path.of(resourcePath)
    private val watcher: WatchService = FileSystems.getDefault.newWatchService()
    private val keys                  = new mutable.HashMap[String, (ResourceKey, WatchKey)]()

    @volatile private var alive = false

    def startWatchService(): Unit = {
        if (alive)
            throw new IllegalStateException("This Resource folder event listener is already alive !")
        alive = true
        new Thread(() => {
            while (alive) {
                val key    = watcher.take()
                val events = key.pollEvents().asInstanceOf[util.List[WatchEvent[Path]]]
                events.forEach(event => {
                    val path = rootPath.resolve(event.context())

                    val folder = path.getParent
                    if (path.getFileName.toString != ResourceFolderMaintainer.MaintainerFileName) {
                        println(s"file updated ${path}")
                        println(s"in folder $folder")
                        import StandardWatchEventKinds._
                        keys.get(folder.toString).fold() { pair =>
                            val key                    = pair._1
                            val action: String => Unit = event.kind() match {
                                case ENTRY_MODIFY => key.onModify
                                case ENTRY_DELETE => key.onDelete
                                case ENTRY_CREATE => key.onCreate
                            }
                            action(folder.getFileName.toString)
                        }
                    }
                })
                key.reset()
            }
        }, "Resources Maintainers Listener").start()
    }

    def close(): Unit = {
        alive = false
        watcher.close()
    }

    def putMaintainer(maintainer: ResourcesMaintainer, resourceKey: ResourceKey): Unit = {

        val location = maintainer.getResources.getAdapter.getAbsolutePath

        if (keys.contains(location)) {
            keys(location)._2.cancel()
        }

        val path = Path.of(location)
        import StandardWatchEventKinds._
        val watchKey = path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        keys.put(location, (resourceKey, watchKey))
    }

}
