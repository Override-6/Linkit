package fr.linkit.api.connection.cache.obj.generation

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.description.SyncObjectSuperClassDescription

import scala.reflect.runtime.universe
import scala.reflect.{ClassTag, classTag}

/**
 * This class generates a class that extends
 * */
trait ObjectWrapperClassCenter {

    def getWrapperClass[S](clazz: Class[S]): Class[S with SynchronizedObject[S]]

    def getWrapperClass[S](desc: SyncObjectSuperClassDescription[S]): Class[S with SynchronizedObject[S]]

    def getClass[S: universe.TypeTag : ClassTag]: Class[S with SynchronizedObject[S]] = getWrapperClass[S](classTag[S].runtimeClass.asInstanceOf[Class[S]])

    def preGenerateClasses(descriptions: List[SyncObjectSuperClassDescription[_]]): Unit

    def preGenerateClasses(classes: Seq[Class[_]]): Unit

    def isClassGenerated[T: ClassTag]: Boolean = isWrapperClassGenerated(classTag[T].runtimeClass)

    def isWrapperClassGenerated[T](clazz: Class[T]): Boolean

    def isClassGenerated[S <: SynchronizedObject[S]](clazz: Class[S]): Boolean
}
