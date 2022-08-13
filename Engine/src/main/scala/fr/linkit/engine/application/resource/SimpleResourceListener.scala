/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.application.resource

import fr.linkit.api.application.resource.{ResourceKey, ResourceListener, ResourcesMaintainer}
import fr.linkit.api.internal.system.log.AppLoggers

import java.io.Closeable
import java.nio.file._
import java.util
import scala.collection.mutable

class SimpleResourceListener extends ResourceListener with Closeable {

    private val watcher: WatchService = FileSystems.getDefault.newWatchService()
    private val keys                  = new mutable.HashMap[String, (ResourceKey, WatchKey)]()

    @volatile private var alive = false

    def startWatchService(): Unit = {
        if (alive)
            throw new IllegalStateException("This Resource folder event listener is already alive !")
        alive = true
        new Thread(() => {
            try {
                while (alive) {
                    listen()
                }
            } catch {
                case _: ClosedWatchServiceException =>
                    AppLoggers.Resource.trace("Resources Listener has been closed.")
            }
        }, "Resources Maintainers Listener").start()
    }

    private def listen(): Unit = {
        val key    = watcher.take()
        val events = key.pollEvents().asInstanceOf[util.List[WatchEvent[Path]]]
        //println(s"events = ${events.stream().map(_.kind().name()).toArray.mkString("Array(", ", ", ")")}")
        events.forEach(event => {

            val folder = key.watchable().asInstanceOf[Path]
            val path   = folder.resolve(event.context())

            if (path.getFileName.toString != ResourceFolderMaintainer.MaintainerFileName) {
                //println(s"file updated ${path}")
                //println(s"in folder $folder")
                import StandardWatchEventKinds._
                keys.get(folder.toString).fold() { pair =>
                    val key                    = pair._1
                    val action: String => Unit = event.kind() match {
                        case ENTRY_MODIFY => key.onModify
                        case ENTRY_DELETE => key.onDelete
                        case ENTRY_CREATE => key.onCreate
                    }
                    action(path.getFileName.toString)
                }
            }
        })
        key.reset()
    }

    override def close(): Unit = {
        alive = false
        watcher.close()
    }

    override def putMaintainer(maintainer: ResourcesMaintainer,
                               resourceKey: ResourceKey): Unit = {

        val location = maintainer.getResources.getPath.toString

        if (keys.contains(location)) {
            keys(location)._2.cancel()
        }

        val path = Path.of(location)
        import StandardWatchEventKinds._
        val watchKey = path.register(watcher, ENTRY_DELETE, ENTRY_MODIFY)
        keys.put(location, (resourceKey, watchKey))
    }

}
