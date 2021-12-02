package fr.linkit.api.gnom.network.statics
import scala.language.dynamics

trait ClassStaticAccessor[A <: AnyRef] extends Dynamic {

    //TODO val behavior: SynchronizedStructureBehavior[A]

    def applyDynamic[T](method: String)(args: Any*): T

    def selectDynamic[T](method: String): T

    def updateDynamic(method: String)(arg: Any): Unit

}
