package fr.linkit.api.connection.cache.repo.generation

import fr.linkit.api.connection.cache.repo.{PuppetDescription, PuppetWrapper}

import scala.reflect.{ClassTag, classTag}

/**
 * This class generates a class that extends
 * */
trait PuppetWrapperGenerator {

    def getClass[S](clazz: Class[S]): Class[S with PuppetWrapper[S]]

    def getClass[S: ClassTag]: Class[S with PuppetWrapper[S]] = getClass[S](classTag[S].runtimeClass.asInstanceOf[Class[S]])

    def preGenerateDescs[S](defaultLoader: ClassLoader, descriptions: Seq[PuppetDescription[S]]): Unit

    def preGenerateClasses[S](defaultLoader: ClassLoader, classes: Seq[Class[_ <: S]]): Unit

    def isClassGenerated[T: ClassTag]: Boolean = isWrapperClassGenerated(classTag[T].runtimeClass)

    def isWrapperClassGenerated[T](clazz: Class[T]): Boolean

    def isClassGenerated[S <: PuppetWrapper[S]](clazz: Class[S]): Boolean

    def getClass[S](desc: PuppetDescription[S]): Class[S with PuppetWrapper[S]]
}
