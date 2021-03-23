/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.core.local.plugin

import fr.`override`.linkit.api.local.plugin._
import fr.`override`.linkit.api.local.system.AppException
import fr.`override`.linkit.api.local.system.fsa.{FileAdapter, FileSystemAdapter}
import fr.`override`.linkit.core.local.plugin.SimplePluginExtractor.{MainClassField, PropertyName}
import fr.`override`.linkit.core.local.plugin.fragment.{LinkitPluginFragment, LinkitRemoteFragment}
import java.net.URLClassLoader
import java.nio.file.{NoSuchFileException, NotDirectoryException}
import java.security.AccessControlContext
import java.util.Properties
import java.util.zip.ZipFile

import fr.`override`.linkit.api.local.ApplicationContext
import fr.`override`.linkit.api.local.concurrency.Procrastinator

import scala.collection.mutable.ListBuffer

/**
 * <p>
 * Main class that handles a folder that contains all the extension jars.
 * An Extension jar must contain a file called 'extension.properties'.
 * The only field that must be provided is a field named 'main' which
 * informs the class path of the main [[LinkitPlugin]] class of a plugin.
 * </p>
 * <p>
 * An extension can add multiple [[LinkitPluginFragment]]
 * in order to provide some of its aspects to other relay's extension, or even to other connected relays
 * if the added extension fragment is an instance of [[LinkitRemoteFragment]]
 * </p>
 *
 * @see [[LinkitPlugin]]
 * */
//TODO The package api.extension must receive a remaster according to the plugin loading system.
class SimplePluginExtractor(context: ApplicationContext, fsa: FileSystemAdapter) extends PluginExtractor {

    override def extract(file: String): PluginLoader = {
        val adapter = fsa.getAdapter(file)
        if (adapter.notExists)
            throw new NoSuchFileException(s"$file does not exists.")
        if (!adapter.getPath.endsWith(".jar"))
            throw new IllegalArgumentException(s"provided file '$file' is not a jar file.")
        extract(loadJar(adapter))
    }

    override def extract(clazz: Class[_ <: Plugin]): PluginLoader = {
        extractAll(clazz)
    }

    override def extractAll(folder: String): PluginLoader = {
        val adapter = fsa.getAdapter(folder)
        if (adapter.notExists)
            throw new NoSuchFileException(s"$folder does not exists.")
        if (!adapter.isDirectory)
            throw new NotDirectoryException(s"$folder is not a directory.")

        val content = fsa.list(adapter)
        val paths = content.filter(_.toString.endsWith(".jar"))

        val extensions = ListBuffer.empty[Class[_ <: LinkitPlugin]]

        for (path <- paths) {
            try {
                extensions += loadJar(path)
            } catch {
                case e: AppException => e.printStackTrace()
            }
        }
        extractAll(extensions.toSeq: _*)
    }

    override def extractAll(classes: Class[_ <: Plugin]*): PluginLoader = {
        new SimplePluginLoader(context, classes: _*)
    }

    private def loadJar(adapter: FileAdapter): Class[_ <: LinkitPlugin] = {
        val jarFile = new ZipFile(adapter.getPath)
        val propertyFile = jarFile.getEntry(PropertyName)
        //Checking property presence
        if (propertyFile == null)
            throw PluginLoadException(s"Jar file $adapter must have a file called '$PropertyName' in his root")

        //Checking property content
        val property = new Properties()
        property.load(jarFile.getInputStream(propertyFile))
        val className = property.getProperty(MainClassField)
        if (className == null)
            throw PluginLoadException(s"Jar file $adapter properties' must contains a field named '$MainClassField'")

        //Loading extension's main
        val classLoader = new URLClassLoader(Array(adapter.toUri.toURL))
        val clazz = classLoader.loadClass(className)
        loadClass(clazz)
    }

    private def loadClass(clazz: Class[_]): Class[_ <: LinkitPlugin] = {
        if (!classOf[LinkitPlugin].isAssignableFrom(clazz)) {
            throw new AppException(s"Class '$clazz' must extends '${classOf[LinkitPlugin]}' to be loaded as a Relay extension.")
        }
        clazz.asInstanceOf[Class[_ <: LinkitPlugin]]
    }


}

object SimplePluginExtractor {
    private val PropertyName = "plugin.properties"
    private val MainClassField = "main"
}