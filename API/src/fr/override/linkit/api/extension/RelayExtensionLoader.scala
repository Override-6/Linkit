package fr.`override`.linkit.api.`extension`

import java.io.Closeable
import java.net.URLClassLoader
import java.util.Properties
import java.util.zip.ZipFile

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.`extension`.LoadPhase._
import fr.`override`.linkit.api.`extension`.RelayExtensionLoader.{MainClassField, PropertyName}
import fr.`override`.linkit.api.`extension`.fragment.FragmentHandler
import fr.`override`.linkit.api.exception.{ExtensionLoadException, RelayException}
import fr.`override`.linkit.api.system.fsa.FileAdapter

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal


class RelayExtensionLoader(relay: Relay) extends Closeable {

    private val configuration = relay.configuration
    private val fsa = configuration.fsAdapter
    private val extensionsFolder = fsa.getAdapter(configuration.extensionsFolder)

    val fragmentHandler = new FragmentHandler(relay, this)

    private var classLoader: URLClassLoader = _
    private val loadedExtensions = ListBuffer.empty[RelayExtension]
    private var phase: LoadPhase = INACTIVE

    override def close(): Unit = {
        phase = DISABLE
        fragmentHandler.destroyFragments()
        loadedExtensions.foreach(_.onDisable())
        loadedExtensions.clear()
        phase = CLOSE
    }

    def launch(): Unit = {
        if (phase != INACTIVE)
            throw new IllegalStateException("Extension loader is currently working")

        fsa.createDirectories(extensionsFolder)
        val content = fsa.list(extensionsFolder)
        val paths = content.filter(_.toString.endsWith(".jar"))
        val urls = paths.map(_.toUri.toURL)

        classLoader = new URLClassLoader(urls, if (classLoader == null) getClass.getClassLoader else classLoader)

        val extensions = ListBuffer.empty[Class[_ <: RelayExtension]]

        for (path <- paths) {
            try {
                extensions += loadJar(path)
            } catch {
                case e: RelayException => e.printStackTrace()
            }
        }
        loadAllExtensions(extensions.toSeq)
    }

    private def loadJar(path: FileAdapter): Class[_ <: RelayExtension] = {
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

    private def loadClass(clazz: Class[_]): Class[_ <: RelayExtension] = {
        if (!classOf[RelayExtension].isAssignableFrom(clazz)) {
            throw new RelayException(s"Class '$clazz' must extends '${classOf[RelayExtension]}' to be loaded as a Relay extension.")
        }
        clazz.asInstanceOf[Class[_ <: RelayExtension]]
    }

    def loadExtension[T <: RelayExtension](clazz: Class[T]): T = {
        if (phase == CLOSE)
            throw new IllegalStateException("This loader is closed !")
        if (phase != ACTIVE && phase != INACTIVE)
            throw new IllegalStateException("Loader must be active or inactive in order to load a specific extension")

        val name = clazz.getSimpleName
        try {
            val constructor = clazz.getConstructor(classOf[Relay])
            constructor.setAccessible(true)
            val extension = constructor.newInstance(relay)

            phase = LOAD
            extension.onLoad()
            phase = ENABLE
            fragmentHandler.startFragments(clazz)
            extension.onEnable()
            phase = ACTIVE

            println(s"Relay extension $name loaded successfully !")
            loadedExtensions.addOne(extension)

            extension
        } catch {
            case _: NoSuchMethodException =>
                throw new RelayException(s"Could not load '$name : Constructor(Relay) is missing !")
            case NonFatal(e) => throw ExtensionLoadException(s"An exception was thrown when loading $name", e)
        }
    }

    def loadExtensions(classes: Class[_ <: RelayExtension]*): Unit =
        loadAllExtensions(classes)

    private def loadAllExtensions(extensions: Seq[Class[_ <: RelayExtension]]): Unit = {
        val instances = (for (clazz <- extensions) yield {
            try {
                clazz.getConstructor(classOf[Relay]).newInstance(relay)
            } catch {
                case _: NoSuchMethodException =>
                    throw new RelayException(s"Could not load '${clazz.getSimpleName}' : Constructor(Relay) is missing !")
            }
        }).to(ListBuffer)

        def perform(action: RelayExtension => Unit): Unit = {
            for (i <- ListBuffer.from(instances).indices) {
                try {
                    val extension = instances(i)
                    action(extension)
                    if (phase == ENABLE)
                        println(`extension`.name + " Enabled successfully !")
                } catch {
                    case NonFatal(e) =>
                        instances.remove(i)
                        throw ExtensionLoadException(s"Could not start extension : Exception thrown during phase '$phase'", e)
                }
            }
        }

        phase = LOAD
        perform(e => e.onLoad())
        phase = ENABLE
        println("Starting all fragments...")
        val count = fragmentHandler.startFragments()
        println(s"$count Fragment started !")
        perform(e => e.onEnable())
        phase = ACTIVE

        loadedExtensions.addAll(instances)

    }

    def getPhase: LoadPhase = phase

}

object RelayExtensionLoader {
    private val PropertyName = "extension.properties"
    private val MainClassField = "main"
}