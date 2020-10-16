package fr.overridescala.vps.ftp.api.task.ext

import java.net.{URL, URLClassLoader}
import java.nio.file.{Files, Path}
import java.util.Properties
import java.util.stream.Collectors
import java.util.zip.ZipFile

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.TaskExtensionLoadException
import fr.overridescala.vps.ftp.api.task.ext.TaskLoader.{MainClassField, PropertyName}

import scala.collection.mutable.ListBuffer


class TaskLoader(relay: Relay, val tasksFolder: Path) {

    type TaskExtensionClass = Class[_ <: TaskExtension]

    private var classLoader: URLClassLoader = null
    private val extensions = ListBuffer.empty[TaskExtensionClass]

    def getLoadedExtensions: Array[TaskExtensionClass] = {
        extensions.toArray
    }

    def refreshTasks(): Unit = {
        Files.createDirectories(tasksFolder)
        val content = Files.list(tasksFolder)
        val paths = content
                .filter(p => p.toString.endsWith(".jar"))
                .collect(Collectors.toList[Path])
        val urls = paths
                .stream()
                .map(_.toUri.toURL)
                .toArray[URL](new Array[URL](_))
        classLoader = new URLClassLoader(urls, getClass.getClassLoader)
        paths.forEach(loadJar)
    }

    private def loadJar(path: Path): Unit = {
        val jarFile = new ZipFile(path.toFile)
        val propertyFile = jarFile.getEntry(PropertyName)
        //Checking property presence
        if (propertyFile == null)
            throw TaskExtensionLoadException(s"jar file $path must have a file called '$PropertyName' in his root")

        //Checking property content
        val property = new Properties(0)
        property.load(jarFile.getInputStream(propertyFile))
        val className = property.getProperty(MainClassField)
        if (className == null)
            throw TaskExtensionLoadException(s"jar file $path properties' must contains a field named '$MainClassField'")

        //Loading extension's main
        val clazz = Class.forName(className, false, classLoader)
        loadClass(clazz)
    }

    private def loadClass(clazz: Class[_]): Unit = {
        if (!classOf[TaskExtension].isAssignableFrom(clazz)) {
            clazz.getDeclaredClasses.foreach(loadClass)
            return
        }
        loadExtension(clazz.asInstanceOf[TaskExtensionClass])
    }

    private def loadExtension(clazz: TaskExtensionClass): Unit = {
        val className = clazz.getName
        try {
            val constructor = clazz.getDeclaredConstructor(classOf[Relay])
            constructor.setAccessible(true)
            constructor.newInstance(relay).main()
        } catch {
            case _: NoSuchMethodException => Console.err.println(s"Could not load Task extension $className, missing Constructor(Relay)")
        }
    }

}

object TaskLoader {
    private val PropertyName = "task_extension.properties"
    private val MainClassField = "main"
}

