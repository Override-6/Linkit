/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.cache.sync.behavior

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.behavior.member.MemberBehaviorFactory
import fr.linkit.api.gnom.cache.sync.behavior.member.field.FieldBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.parameter.ParameterBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.returnvalue.ReturnValueBehavior
import fr.linkit.api.gnom.cache.sync.invokation.local.LocalMethodInvocation
import fr.linkit.api.gnom.network.Engine

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * A store of [[ObjectBehavior]], used by [[fr.linkit.api.gnom.cache.sync.tree.SynchronizedObjectTree]]
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
