/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.cache.obj.behavior

import java.util

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.behavior.SynchronizedObjectBehavior
import fr.linkit.api.connection.cache.obj.behavior.annotation.BasicInvocationRule
import fr.linkit.api.connection.cache.obj.behavior.member.MemberBehaviorFactory
import fr.linkit.api.connection.cache.obj.behavior.member.field.{FieldBehavior, FieldModifier}
import fr.linkit.api.connection.cache.obj.behavior.member.method.InternalMethodBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.parameter.ParameterBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.returnvalue.ReturnValueBehavior
import fr.linkit.api.connection.cache.obj.description.{FieldDescription, MethodDescription, SyncObjectSuperclassDescription}
import fr.linkit.api.connection.network.Engine
import fr.linkit.api.local.concurrency.Procrastinator
import fr.linkit.engine.connection.cache.obj.behavior.SynchronizedObjectBehaviorBuilder.{FieldControl, MethodControl}
import fr.linkit.engine.connection.cache.obj.behavior.member.{MethodParameterBehavior, SyncMethodBehavior}
import fr.linkit.engine.connection.cache.obj.description.SyncObjectClassDescription
import fr.linkit.engine.connection.cache.obj.invokation.remote.{DefaultRMIHandler, InvokeOnlyRMIHandler}
import org.jetbrains.annotations.Nullable

import scala.collection.mutable
import scala.reflect.ClassTag

abstract class SynchronizedObjectBehaviorBuilder[T <: AnyRef] private(val classDesc: SyncObjectSuperclassDescription[T]) {

    protected val methodsMap = mutable.HashMap.empty[MethodDescription, MethodControl]
    protected val fieldsMap  = mutable.HashMap.empty[FieldDescription, FieldControl[Any]]
    protected var asField      : Option[FieldBehavior[T]] = None
    protected var asParameter  : Option[ParameterBehavior[T]] = None
    protected var asReturnValue: Option[ReturnValueBehavior[T]] = None

    def this()(implicit classTag: ClassTag[T]) = {
        this(SyncObjectClassDescription[T](classTag.runtimeClass.asInstanceOf[Class[T]]))
    }

    final def annotateMethod(name: String, params: Class[_]*): MethodModification = {
        val methodID = name.hashCode + util.Arrays.hashCode(params.toArray[AnyRef])
        val method   = classDesc.findMethodDescription(methodID).getOrElse(throw new NoSuchElementException(s"Method description '$name' not found."))
        new MethodModification(Seq(method))
    }

    final def annotateAllMethods(name: String): MethodModification = {
        new MethodModification(
            classDesc.listMethods()
                .filter(_.symbol.name.toString == name)
        )
    }

    final def annotateAllMethods: MethodModification = {
        new MethodModification(classDesc.listMethods())
    }

    def build(factory: MemberBehaviorFactory): SynchronizedObjectBehavior[T] = {
        new DefaultSynchronizedObjectBehavior[T](classDesc, factory, asField, asParameter, asReturnValue) {
            override protected def generateMethodsBehavior(): Iterable[InternalMethodBehavior] = {
                classDesc.listMethods()
                    .map(genMethodBehavior(factory, _))
            }

            override protected def generateFieldsBehavior(): Iterable[FieldBehavior[Any]] = {
                //TODO Handle field customisation as well
                super.generateFieldsBehavior()
            }
        }
    }

    private def genMethodBehavior(factory: MemberBehaviorFactory, desc: MethodDescription): InternalMethodBehavior = {
        val opt = methodsMap.get(desc)
        if (opt.isEmpty)
            factory.genMethodBehavior(None, desc)
        else {
            val control = opt.get
            import control._
            val handler = if (invokeOnly) InvokeOnlyRMIHandler else DefaultRMIHandler
            val params  = if (synchronizedParams == null) AnnotationBasedMemberBehaviorFactory.getSynchronizedParams(desc.javaMethod) else synchronizedParams.toArray.asInstanceOf[Array[ParameterBehavior[Any]]]
            SyncMethodBehavior(desc, params, synchronizeReturnValue, hide, Array(value), procrastinator, handler)
        }
    }

    class MethodModification private[SynchronizedObjectBehaviorBuilder](descs: Iterable[MethodDescription]) {

        def by(control: MethodControl): this.type = {
            descs.foreach(methodsMap.put(_, control))
            this
        }

        def and(otherName: String): MethodModification = {
            new MethodModification(descs ++ classDesc.listMethods()
                .filter(_.symbol.name.toString == otherName))
        }
    }

    class FieldModification private[SynchronizedObjectBehaviorBuilder](descs: Iterable[FieldDescription]) {
        def by(control: FieldControl[Any]): this.type = {
            descs.foreach(fieldsMap.put(_, control))
            this
        }

        def and(otherName: String): FieldModification = {
            new FieldModification(descs ++ classDesc.listFields()
                .filter(_.javaField.getName == otherName))
        }
    }

}

object SynchronizedObjectBehaviorBuilder {

    case class MethodControl(value: BasicInvocationRule,
                             synchronizeReturnValue: Boolean = false,
                             invokeOnly: Boolean = false,
                             hide: Boolean = false,
                             synchronizedParams: Seq[MethodParameterBehavior[_]] = null,
                             @Nullable procrastinator: Procrastinator = null)

    case class FieldControl[A](isActivated: Boolean) extends FieldModifier[A] {
        override def forLocalComingFromLocal(localField: A, containingObject: SynchronizedObject[_]): A = localField

        override def forLocalComingFromRemote(receivedField: A, containingObject: SynchronizedObject[_], remote: Engine): A = receivedField

        override def forRemote(localField: A, containingObject: SynchronizedObject[_], remote: Engine): A = localField
    }

}
