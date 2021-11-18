package fr.linkit.api.gnom.network.statics
import scala.language.dynamics

trait ClassStaticAccessor[A <: AnyRef] extends Dynamic {

    //TODO val behavior: SynchronizedStructureBehavior[A]

    protected def applyDynamic(method: String)(args: Any*): Any

    protected def selectDynamic(method: String): Any

    protected def updateDynamic(method: String)(arg: Any): Unit

}
