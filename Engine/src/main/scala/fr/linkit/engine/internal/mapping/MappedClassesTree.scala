package fr.linkit.engine.internal.mapping

import scala.collection.mutable

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
            val result = classes.get(className)
            if (result.nonEmpty)
                return result
            for ((_, pck) <- subPackages) {
                val opt = pck.findClass(className)
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
            val idx = pckName.indexOf('.', bound)
            val first = pckName.take(if (idx == -1) pckName.length else idx)
            subPackages.get(first).flatMap(_.findPackage(pckName))
        }

        private[MappedClassesTree] def addClass(clazz: Class[_]): Unit = {
            val classPackage = clazz.getPackageName
            if (classPackage == name) {
                classes.put(clazz.getSimpleName, clazz)
                return
            }
            if (!classPackage.startsWith(name))
                throw new IllegalArgumentException(s"could not add $clazz to package item '$name': class package is not a child of this package.")
            val idx = classPackage.indexOf('.', name.length + 1)
            val nextPackageName = if (idx == -1) classPackage else classPackage.take(idx)
            val nextPackage = subPackages.getOrElseUpdate(nextPackageName, PackageItem(nextPackageName))
            nextPackage.addClass(clazz)
        }
    }

}
