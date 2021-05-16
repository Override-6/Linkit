package fr.linkit.api.connection.cache.repo.generation

import fr.linkit.api.connection.cache.repo.{PuppetDescription, PuppetWrapper}

import scala.reflect.{ClassTag, classTag}

/**
 * This class generates a class that extends
 * */
trait PuppetWrapperGenerator {

    def getClass[S](clazz: Class[S]): Class[S with PuppetWrapper[S]]

    def getClass[S : ClassTag]: Class[S with PuppetWrapper[S]] = getClass[S](classTag[S].runtimeClass.asInstanceOf[Class[S]])

    def preGenerateClasses[S](classes: Class[_ <: S]*): Unit

    def preGenerateDescs[S](descriptions: Seq[PuppetDescription[S]]): Unit

    def isClassGenerated[T : ClassTag]: Boolean = isClassGenerated(classTag[T].runtimeClass)

    def isClassGenerated[T](clazz: Class[T]): Boolean

}
