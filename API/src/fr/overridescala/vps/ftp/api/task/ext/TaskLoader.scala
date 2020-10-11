package fr.overridescala.vps.ftp.api.task.ext

import java.io.File
import java.net.{URL, URLClassLoader}
import java.nio.file.{Files, Path}
import java.util.stream.Collectors
import java.util.zip.ZipInputStream

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.TaskCompleterHandler


class TaskLoader(relay: Relay, tasksFolder: Path) {

    private var classLoader: URLClassLoader = null

    def loadTasks(): Unit = {
        Files.createDirectories(tasksFolder)
        println(tasksFolder)
        val urls = Files.list(tasksFolder)
                .filter(p => p.endsWith(".jar") || p.endsWith(".class"))
                .map(_.toUri.toURL)
                .collect(Collectors.toList[URL])
                .toArray(Array(): Array[URL])
        classLoader = new URLClassLoader(urls, getClass.getClassLoader)
    }


    private def loadJar(path: Path): Unit = {
        val jar = new ZipInputStream(Files.newInputStream(path))
        val entry = jar.getNextEntry
        while (entry != null) {
            if (!entry.isDirectory && entry.getName.endsWith(".class")) {
                val className = entry.getName.replace(File.separatorChar, '.')
                val clazz = Class.forName(className, false, classLoader)
                loadClass(clazz)
            }
        }
    }

    private def loadClass(clazz: Class[_]): Unit = {
        if (!classOf[TaskExtension].isAssignableFrom(clazz)) {
            clazz.getDeclaredClasses.foreach(loadClass)
            return
        }
        loadExtension(clazz.asInstanceOf[Class[_ <: TaskExtension]])
    }

    private def loadExtension(clazz: Class[_ <: TaskExtension]): Unit = {
        val className = clazz.getName
        try {
            val constructor = clazz.getDeclaredConstructor(classOf[TaskCompleterHandler])
            constructor.setAccessible(true)
            constructor.newInstance(relay.getTaskCompleterHandler).main()
        } catch {
            case _: NoSuchMethodException => Console.err.println(s"Could not load Task extension $className")
        }
    }

    private def loadPath(path: Path): Unit = {
        val extension = path.toString.split(".").last
        if (extension == ".jar" || extension == ".class") {
            extension match {
                case ".jar" => loadJar(path)
                case ".class" => loadClass(Class.forName(path.toFile.getName, false, classLoader))
            }
        }
    }

}

