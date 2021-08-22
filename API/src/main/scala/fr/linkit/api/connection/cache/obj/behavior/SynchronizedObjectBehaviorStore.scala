package fr.linkit.api.connection.cache.obj.behavior

import fr.linkit.api.connection.cache.obj.behavior.member.MemberBehaviorFactory

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * A store of [[SynchronizedObjectBehavior]], used by [[fr.linkit.api.connection.cache.obj.tree.SynchronizedObjectTree]]
 * */
//TODO This class is not furthermore documented, some change will be made and it's development is not yet completely ended.
trait SynchronizedObjectBehaviorStore {

    val factory: MemberBehaviorFactory

    def get[B <: AnyRef: TypeTag : ClassTag]: SynchronizedObjectBehavior[B]

    def getFromClass[B <: AnyRef](clazz: Class[_]): SynchronizedObjectBehavior[B]

    def findFromClass[B <: AnyRef](clazz: Class[_]): Option[SynchronizedObjectBehavior[B]]

}
