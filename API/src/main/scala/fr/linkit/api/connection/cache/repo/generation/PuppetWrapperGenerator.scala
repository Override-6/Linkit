package fr.linkit.api.connection.cache.repo.generation

import com.sun.jdi.ClassType
import fr.linkit.api.connection.cache.repo.PuppetWrapper
import fr.linkit.api.connection.cache.repo.description.PuppetDescription

import scala.reflect.runtime.universe
import scala.reflect.{ClassTag, classTag}

/**
 * This class generates a class that extends
 * */
trait PuppetWrapperGenerator {

    def getClass[S: TypeTag](clazz: Class[S]): Class[S with PuppetWrapper[S]]

    def getClass[S: universe.TypeTag]: Class[S with PuppetWrapper[S]] = getClass[S](classTag[S].runtimeClass.asInstanceOf[Class[S]])

    def preGenerateDescs[S](defaultLoader: ClassLoader, descriptions: Seq[PuppetDescription[S]]): Unit

    def isClassGenerated[T: ClassTag]: Boolean = isWrapperClassGenerated(classTag[T].runtimeClass)

    def isWrapperClassGenerated[T](clazz: Class[T]): Boolean

    def isClassGenerated[S <: PuppetWrapper[S]](clazz: Class[S]): Boolean

    def getClass[S: universe.TypeTag](desc: PuppetDescription[S]): Class[S with PuppetWrapper[S]]

    def preGenerateClasses[S: universe.TypeTag](defaultLoader: ClassLoader, classes: Seq[Class[_ <: S]]): Unit
}
