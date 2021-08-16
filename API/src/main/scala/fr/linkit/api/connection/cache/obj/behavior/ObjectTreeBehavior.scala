package fr.linkit.api.connection.cache.obj.behavior

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

trait ObjectTreeBehavior {

    val factory: MemberBehaviorFactory

    def get[B: TypeTag : ClassTag]: SynchronizedObjectBehavior[B]

    def getFromClass[B](clazz: Class[_]): SynchronizedObjectBehavior[B]

}
