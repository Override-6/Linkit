package fr.overridescala.linkkit.api.`extension`

import java.net.URLClassLoader
import java.nio.file.{Files, Path}
import java.util.Properties
import java.util.stream.Collectors
import java.util.zip.ZipFile

import fr.overridescala.linkkit.api.Relay
import fr.overridescala.linkkit.api.`extension`.RelayExtensionLoader.{MainClassField, PropertyName}
import fr.overridescala.linkkit.api.exceptions.{RelayException, TaskExtensionLoadException}

import scala.collection.mutable.ListBuffer


class RelayExtensionLoader(relay: Relay, val extensionsFolder: Path) {

    type A = Class[_ <: RelayExtension]

    private val notifier = relay.eventObserver.notifier
    private var classLoader: URLClassLoader = _

    def loadExtensions(): Unit = {
        Files.createDirectories(extensionsFolder)
        val content = Files.list(extensionsFolder)
        val paths = content
                .filter(_.toString.endsWith(".jar"))
                .collect(Collectors.toList[Path])
                .toArray(new Array[Path](0))
        val urls = paths
                .map(_.toUri.toURL)
        classLoader = new URLClassLoader(urls, if (classLoader == null) getClass.getClassLoader else classLoader)

        val extensions = ListBuffer.empty[ExtensionInfo]

        for (path <- paths) {
            try {
                extensions += loadJar(path)
            } catch {
                case e: RelayException => e.printStackTrace()
            }
        }
        ExtensionLoaderNode.loadGraph(relay, extensions.toSeq)
    }

    def loadExtension[T <: RelayExtension](clazz: Class[T]): T = {
        val name = retrieveInfo(clazz).name
        try {
            val constructor = clazz.getConstructor(classOf[Relay])
            constructor.setAccessible(true)
            val extension = constructor.newInstance(relay)
            notifier.onExtensionLoaded(extension)
            println(s"Relay extension $name loaded successfully !")
            extension.main()
            extension
        } catch {
            case _: NoSuchMethodException =>
                throw new RelayException(s"Could not load '$name : Constructor(Relay) is missing !")
        }
    }

    private def loadJar(path: Path): ExtensionInfo = {
        val jarFile = new ZipFile(path.toFile)
        val propertyFile = jarFile.getEntry(PropertyName)
        //Checking property presence
        if (propertyFile == null)
            throw TaskExtensionLoadException(s"jar file $path must have a file called '$PropertyName' in his root")

        //Checking property content
        val property = new Properties()
        property.load(jarFile.getInputStream(propertyFile))
        val className = property.getProperty(MainClassField)
        if (className == null)
            throw TaskExtensionLoadException(s"jar file $path properties' must contains a field named '$MainClassField'")

        //Loading extension's main
        val clazz = classLoader.loadClass(className)
        loadClass(clazz)
    }

    private def loadClass(clazz: Class[_]): ExtensionInfo = {
        if (!classOf[RelayExtension].isAssignableFrom(clazz)) {
            throw new RelayException(s"class '$clazz' must extends '${classOf[RelayExtension]}' to be loaded as a Relay extension.")
        }
        retrieveInfo(clazz.asInstanceOf[A])
    }

    private def retrieveInfo(extClass: A): ExtensionInfo = {
        if (extClass.isAnnotationPresent(classOf[relayExtensionInfo])) {
            val annotation = extClass.getAnnotation(classOf[relayExtensionInfo])
            var name = annotation.name()
            if (name.isEmpty) name = extClass.getSimpleName
            return ExtensionInfo(annotation.dependencies, name, extClass)
        }
        ExtensionInfo(Array(), extClass.getSimpleName, extClass)
    }

}

object RelayExtensionLoader {
    private val PropertyName = "extension.properties"
    private val MainClassField = "main"
}