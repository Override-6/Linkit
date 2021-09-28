package fr.linkit.api.gnom.cache.sync.generation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.description.SyncObjectSuperclassDescription

import scala.reflect.runtime.universe
import scala.reflect.{ClassTag, classTag}

/**
 * This class generates a class that extends
 * */
trait SyncClassCenter {

    def getSyncClass[S <: AnyRef](clazz: Class[_]): Class[S with SynchronizedObject[S]]

    def getSyncClassFromDesc[S<: AnyRef](desc: SyncObjectSuperclassDescription[S]): Class[S with SynchronizedObject[S]]

    def getSyncClassFromTpe[S<: AnyRef: universe.TypeTag : ClassTag]: Class[S with SynchronizedObject[S]] = getSyncClass[S](classTag[S].runtimeClass.asInstanceOf[Class[S]])

    def preGenerateClasses(descriptions: List[SyncObjectSuperclassDescription[_]]): Unit

    def preGenerateClasses(classes: Seq[Class[_]]): Unit

    def isClassGenerated[T: ClassTag]: Boolean = isWrapperClassGenerated(classTag[T].runtimeClass)

    def isWrapperClassGenerated[T](clazz: Class[T]): Boolean

    def isClassGenerated[S <: SynchronizedObject[S]](clazz: Class[S]): Boolean
}
