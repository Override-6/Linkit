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

package linkit.base.mapping

import fr.linkit.api.internal.system.log.AppLoggers

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object MappedClassesTree {

    private val root = PackageItem("")

    private[mapping] def addClass(clazz: Class[_]): Unit = {
        root.addClass(clazz)
    }

    def getClass(rootPackage: String, className: String): Option[Class[_]] = {
        root.findPackage(rootPackage).flatMap(_.findClass(className))
    }

    case class PackageItem(name: String) {
        val lastItemName = if (name.isEmpty) "" else name.split('.').last
        private val classes     = mutable.HashMap.empty[String, Class[_]]
        private val subPackages = mutable.HashMap.empty[String, PackageItem]

        def findClass(className: String): Option[Class[_]] = {
            findClass0(className.split('.').last)
        }

        private def findClass0(className: String): Option[Class[_]] = {
            val result = classes.get(className)
            if (result.nonEmpty)
                return result
            for ((_, pck) <- subPackages) {
                val opt = pck.findClass0(className)
                if (opt.nonEmpty)
                    return opt
            }
            None
        }

        def findPackage(pckName: String): Option[PackageItem] = {
            if (pckName == name)
                return Some(this)
            if (pckName.length < name.length)
                throw new IllegalArgumentException("pckName.length <= this package name length")

            val bound = if (name.isEmpty) name.length else name.length + 1
            val idx   = pckName.indexOf('.', bound)
            val first = pckName.take(if (idx == -1) pckName.length else idx)
            subPackages.get(first).flatMap(_.findPackage(pckName))
        }

        private def getFullPackageName(clazz: Class[_]): String = {
            val packageName = clazz.getPackageName
            var c           = clazz.getDeclaringClass
            if (c == null) {
                return packageName
            }

            val classes = ListBuffer.empty[String]
            while (c != null) {
                classes += c.getSimpleName
                c = c.getDeclaringClass
            }
            packageName + "." + classes.reverse.mkString(".")
        }

        private[MappedClassesTree] def addClass(clazz: Class[_]): Unit = {
            addClass(getFullPackageName(clazz), clazz)
        }

        private def addClass(classPackage: String, clazz: Class[_]): Unit = {
            if (classPackage == name) {
                classes.put(clazz.getSimpleName, clazz)
                for (inner <- clazz.getDeclaredClasses) {
                    val nextPackageName = classPackage + "." + clazz.getSimpleName
                    val nextPackage     = subPackages.getOrElseUpdate(nextPackageName, PackageItem(nextPackageName))
                    nextPackage.addClass(nextPackageName, inner)
                }
                return
            }
            if (!classPackage.startsWith(name))
                throw new IllegalArgumentException(s"could not add $clazz to package item '$name': class package is not a child of this package.")
            val idx             = classPackage.indexOf('.', name.length + 1)
            val nextPackageName = if (idx == -1) classPackage else classPackage.take(idx)
            val nextPackage     = subPackages.getOrElseUpdate(nextPackageName, PackageItem(nextPackageName))
            nextPackage.addClass(classPackage, clazz)
        }


    }

}
