package fr.`override`.linkit.api.`extension`

import java.io.Closeable
import java.net.URLClassLoader
import java.util.Properties
import java.util.zip.ZipFile

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.`extension`.RelayExtensionLoader.{MainClassField, PropertyName}
import fr.`override`.linkit.api.exception.{ExtensionLoadException, RelayException}
import fr.`override`.linkit.api.system.fsa.FileAdapter

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal


class RelayExtensionLoader(relay: Relay) extends Closeable {

    type A = Class[_ <: RelayExtension]

    private val configuration = relay.configuration
    private val fsa = configuration.fsAdapter
    private val notifier = relay.eventObserver.notifier
    private val loadedExtensions = ListBuffer.empty[(RelayExtension, ExtensionInfo)]
    private val extensionsFolder = fsa.getAdapter(configuration.extensionsFolder)

    private var classLoader: URLClassLoader = _

    override def close(): Unit = {
        loadedExtensions.foreach(extensionTuple => {
            val extension = extensionTuple._1
            val info = extensionTuple._2
            println(s"Disabling '${info.name}...")
            extension.onDisable()
        })
        loadedExtensions.clear()
    }

    def loadExtensions(): Unit = {
        fsa.createDirectories(extensionsFolder)
        val content = fsa.list(extensionsFolder)
        val paths = content.filter(_.toString.endsWith(".jar"))
        val urls = paths.map(_.toUri.toURL)

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
        val info = retrieveInfo(clazz)
        val name = info.name
        try {
            val constructor = clazz.getConstructor(classOf[Relay])
            constructor.setAccessible(true)
            val extension = constructor.newInstance(relay)
            notifier.onExtensionLoaded(extension)
            extension.onEnable()
            println(s"Relay extension $name loaded successfully !")
            loadedExtensions.addOne(extension, info)

            extension
        } catch {
            case _: NoSuchMethodException =>
                throw new RelayException(s"Could not load '$name : Constructor(Relay) is missing !")
            case NonFatal(e) => throw ExtensionLoadException(s"An exception was thrown when loading $name", e)
        }
    }

    private def loadJar(path: FileAdapter): ExtensionInfo = {
        val jarFile = new ZipFile(path.getPath)
        val propertyFile = jarFile.getEntry(PropertyName)
        //Checking property presence
        if (propertyFile == null)
            throw ExtensionLoadException(s"Jar file $path must have a file called '$PropertyName' in his root")

        //Checking property content
        val property = new Properties()
        property.load(jarFile.getInputStream(propertyFile))
        val className = property.getProperty(MainClassField)
        if (className == null)
            throw ExtensionLoadException(s"Jar file $path properties' must contains a field named '$MainClassField'")

        //Loading extension's main
        val clazz = classLoader.loadClass(className)
        loadClass(clazz)
    }

    private def loadClass(clazz: Class[_]): ExtensionInfo = {
        if (!classOf[RelayExtension].isAssignableFrom(clazz)) {
            throw new RelayException(s"Class '$clazz' must extends '${classOf[RelayExtension]}' to be loaded as a Relay extension.")
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