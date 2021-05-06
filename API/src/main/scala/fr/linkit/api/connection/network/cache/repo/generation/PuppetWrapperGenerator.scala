package fr.linkit.api.connection.network.cache.repo.generation

import fr.linkit.api.connection.network.cache.repo.{PuppetDescription, PuppetWrapper}

import scala.reflect.{ClassTag, classTag}

/**
 * This class generates a class that extends
 * */
trait PuppetWrapperGenerator {

    def getClass[S <: Serializable](clazz: Class[S]): Class[S with PuppetWrapper[S]]

    def preGenerateClasses[S <: Serializable](classes: Class[_ <: S]*): Unit

    def preGenerateDescs[S <: Serializable](descriptions: Seq[PuppetDescription[S]]): Unit

    def isClassGenerated[T : ClassTag]: Boolean = isClassGenerated(classTag[T].runtimeClass)

    def isClassGenerated[T](clazz: Class[T]): Boolean

}
