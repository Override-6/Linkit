package fr.linkit.api.connection.cache.obj.behavior

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

trait ObjectTreeBehavior {

    val factory: MemberBehaviorFactory

    def get[B: TypeTag : ClassTag]: WrapperBehavior[B]

    def getFromClass[B](clazz: Class[_]): WrapperBehavior[B]

    def put[B](clazz: Class[B], bhv: WrapperBehavior[B])
}
