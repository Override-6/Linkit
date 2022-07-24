package fr.linkit.api.gnom.network.statics
import scala.reflect.ClassTag
import scala.language.dynamics
trait StaticAccessor extends Dynamic {

    def applyDynamic[T: ClassTag](name: String)(params: Any*): T
}
