package fr.overridescala.vps.ftp.api.task.ext

import java.net.{URL, URLClassLoader}
import java.nio.file.{Files, Path}
import java.util.Properties
import java.util.stream.Collectors
import java.util.zip.ZipFile

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.TaskExtensionLoadException
import fr.overridescala.vps.ftp.api.task.ext.TaskLoader.{MainClassField, PropertyName}


class TaskLoader(relay: Relay, tasksFolder: Path) {

    private var classLoader: URLClassLoader = null

    def loadTasks(): Unit = {
        Files.createDirectories(tasksFolder)
        val content = Files.list(tasksFolder)
        val paths = content
                .filter(p => p.toString.endsWith(".jar") || p.toString.endsWith(".class"))
                .collect(Collectors.toList[Path])
        val urls = paths
                .stream()
                .map(_.toUri.toURL)
                .toArray[URL](new Array[URL](_))
        classLoader = new URLClassLoader(urls, getClass.getClassLoader)
        paths.forEach(loadPath(_))
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

        //Loading main
        val clazz = Class.forName(className, false, classLoader)
        loadClass(clazz)
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
            val constructor = clazz.getDeclaredConstructor(classOf[Relay])
            constructor.setAccessible(true)
            constructor.newInstance(relay).main()
        } catch {
            case _: NoSuchMethodException => Console.err.println(s"Could not load Task extension $className")
        }
    }

    private def loadPath(path: Path): Unit = {
        val extension = path.toString.split('.').last
        extension match {
            case "jar" => loadJar(path)
            case "class" => loadClass(Class.forName(path.toFile.getName, false, classLoader))
        }
    }

}

object TaskLoader {
    private val PropertyName = "task_extension.properties"
    private val MainClassField = "main"
}

