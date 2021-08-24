package fr.linkit.api.connection.cache.obj.behavior

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.behavior.member.MemberBehaviorFactory
import fr.linkit.api.connection.cache.obj.behavior.member.field.FieldBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.parameter.ParameterBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.returnvalue.ReturnValueBehavior
import fr.linkit.api.connection.cache.obj.invokation.local.LocalMethodInvocation
import fr.linkit.api.connection.network.Engine

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * A store of [[ObjectBehavior]], used by [[fr.linkit.api.connection.cache.obj.tree.SynchronizedObjectTree]]
 * */
//TODO This class is not furthermore documented, some change will be made and it's development is not yet completely ended.
trait ObjectBehaviorStore {

    val factory: MemberBehaviorFactory

    def get[B <: AnyRef : TypeTag : ClassTag]: ObjectBehavior[B]

    def getFromClass[B <: AnyRef](clazz: Class[_]): ObjectBehavior[B]

    def findFromClass[B <: AnyRef](clazz: Class[_]): Option[ObjectBehavior[B]]

    def modifyFieldForLocalComingFromLocal(enclosingObject: SynchronizedObject[_], fieldValue: AnyRef, fieldBhv: FieldBehavior[AnyRef]): AnyRef

    def modifyFieldForLocalComingFromRemote(enclosingObject: SynchronizedObject[_], engine: Engine, fieldValue: AnyRef, fieldBhv: FieldBehavior[AnyRef]): AnyRef

    def modifyReturnValueForLocalComingFromLocal(invocation: LocalMethodInvocation[_], returnValue: AnyRef, returnBhv: ReturnValueBehavior[AnyRef]): AnyRef

    def modifyReturnValueForLocalComingFromRemote(invocation: LocalMethodInvocation[_], engine: Engine, returnValue: AnyRef, returnBhv: ReturnValueBehavior[AnyRef]): AnyRef

    def modifyParameterForLocalComingFromLocal(args: Array[Any], paramValue: AnyRef, paramBhv: ParameterBehavior[AnyRef]): AnyRef

    def modifyParameterForLocalComingFromRemote(invocation: LocalMethodInvocation[_], engine: Engine, paramValue: AnyRef, paramBhv: ParameterBehavior[AnyRef]): AnyRef

    def modifyParameterForRemote(invocation: LocalMethodInvocation[_], remote: Engine, paramValue: AnyRef, paramBhv: ParameterBehavior[AnyRef]): AnyRef

    def modifyReturnValueForRemote(invocation: LocalMethodInvocation[_], remote: Engine, returnValue: AnyRef, returnValueBhv: ReturnValueBehavior[AnyRef]): AnyRef
}
