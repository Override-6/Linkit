package fr.linkit.api.connection.cache.obj.generation

import fr.linkit.api.connection.cache.obj.PuppetWrapper
import fr.linkit.api.local.generation.PuppetClassDescription

import scala.reflect.runtime.universe
import scala.reflect.{ClassTag, classTag}

/**
 * This class generates a class that extends
 * */
trait ObjectWrapperClassCenter {

    def getWrapperClass[S](clazz: Class[S]): Class[S with PuppetWrapper[S]]

    def getWrapperClass[S](desc: PuppetClassDescription[S]): Class[S with PuppetWrapper[S]]

    def getClass[S: universe.TypeTag : ClassTag]: Class[S with PuppetWrapper[S]] = getWrapperClass[S](classTag[S].runtimeClass.asInstanceOf[Class[S]])

    def preGenerateClasses(descriptions: List[PuppetClassDescription[_]]): Unit

    def preGenerateClasses(classes: Seq[Class[_]]): Unit

    def isClassGenerated[T: ClassTag]: Boolean = isWrapperClassGenerated(classTag[T].runtimeClass)

    def isWrapperClassGenerated[T](clazz: Class[T]): Boolean

    def isClassGenerated[S <: PuppetWrapper[S]](clazz: Class[S]): Boolean
}