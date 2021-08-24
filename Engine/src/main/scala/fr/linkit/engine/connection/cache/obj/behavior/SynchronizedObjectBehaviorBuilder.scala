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

import fr.linkit.api.connection.cache.obj.behavior.SynchronizedObjectBehavior
import fr.linkit.api.connection.cache.obj.behavior.annotation.BasicInvocationRule
import fr.linkit.api.connection.cache.obj.behavior.member.MemberBehaviorFactory
import fr.linkit.api.connection.cache.obj.behavior.member.field.{FieldBehavior, FieldModifier}
import fr.linkit.api.connection.cache.obj.behavior.member.method.InternalMethodBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.parameter.{ParameterBehavior, ParameterModifier}
import fr.linkit.api.connection.cache.obj.behavior.member.method.returnvalue.ReturnValueModifier
import fr.linkit.api.connection.cache.obj.description.{FieldDescription, MethodDescription, SyncObjectSuperclassDescription}
import fr.linkit.api.local.concurrency.Procrastinator
import fr.linkit.engine.connection.cache.obj.behavior.SynchronizedObjectBehaviorBuilder.{FieldControl, MethodControl}
import fr.linkit.engine.connection.cache.obj.behavior.member.{MethodParameterBehavior, SyncFieldBehavior, SyncMethodBehavior}
import fr.linkit.engine.connection.cache.obj.description.SimpleSyncObjectSuperClassDescription
import fr.linkit.engine.connection.cache.obj.invokation.remote.{DefaultRMIHandler, InvokeOnlyRMIHandler}
import org.jetbrains.annotations.Nullable

import java.util
import scala.collection.mutable
import scala.reflect.ClassTag

abstract class SynchronizedObjectBehaviorBuilder[T <: AnyRef] private(val classDesc: SyncObjectSuperclassDescription[T]) {

    protected val methodsMap                            = mutable.HashMap.empty[MethodDescription, MethodControl]
    protected val fieldsMap                             = mutable.HashMap.empty[FieldDescription, FieldControl[Any]]
    protected var asField      : FieldModifier[T]       = _
    protected var asParameter  : ParameterModifier[T]   = _
    protected var asReturnValue: ReturnValueModifier[T] = _

    def this()(implicit classTag: ClassTag[T]) = {
        this(SimpleSyncObjectSuperClassDescription[T](classTag.runtimeClass.asInstanceOf[Class[T]]))
    }

    final def annotateMethod(name: String, params: Class[_]*): MethodModification = {
        val methodID = name.hashCode + util.Arrays.hashCode(params.toArray[AnyRef])
        val method   = classDesc.findMethodDescription(methodID).getOrElse(throw new NoSuchElementException(s"Method description '$name' not found."))
        new MethodModification(Seq(method))
    }

    final def annotateAllMethods(name: String): MethodModification = {
        new MethodModification(
            classDesc.listMethods()
                    .filter(_.method.getName == name)
        )
    }

    final def annotateAllMethods: MethodModification = {
        new MethodModification(classDesc.listMethods())
    }

    def build(factory: MemberBehaviorFactory): SynchronizedObjectBehavior[T] = {
        new DefaultSynchronizedObjectBehavior[T](classDesc, factory, Option(asField), Option(asParameter), Option(asReturnValue)) {
            override protected def generateMethodsBehavior(): Iterable[InternalMethodBehavior] = {
                classDesc.listMethods()
                        .map(genMethodBehavior(factory, _))
            }

            override protected def generateFieldsBehavior(): Iterable[FieldBehavior[Any]] = {
                classDesc.listFields()
                        .map(genFieldBehavior(factory, _))
            }
        }
    }

    private def genFieldBehavior(factory: MemberBehaviorFactory, desc: FieldDescription): FieldBehavior[Any] = {
        val opt = fieldsMap.get(desc)
        if (opt.isEmpty)
            factory.genFieldBehavior(desc)
        else {
            val control = opt.get
            SyncFieldBehavior(desc, control.isActivated, control)
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
            val params  = extractParams(control, desc)
            SyncMethodBehavior(desc, params, synchronizeReturnValue, hide, Array(value), procrastinator, handler)
        }
    }

    private def extractParams(control: MethodControl, desc: MethodDescription): Array[ParameterBehavior[Any]] = {
        val javaMethod = desc.method
        val defaults   = AnnotationBasedMemberBehaviorFactory.getSynchronizedParams(javaMethod)
        control.paramMap.foreachEntry((argName, argControl) => {
            val i = defaults.indexWhere(_.getName == argName)
            if (i > 0)
                defaults(i) = new MethodParameterBehavior[Any](argName, argControl.isActivated, argControl)
            else throw new IllegalArgumentException(s"Unknown parameter '$argName' for method ${javaMethod.getName}")
        })
        defaults
    }

    class MethodModification private[SynchronizedObjectBehaviorBuilder](descs: Iterable[MethodDescription]) {

        def by(control: MethodControl): this.type = {
            descs.foreach(methodsMap.put(_, control))
            this
        }

        def and(otherName: String): MethodModification = {
            new MethodModification(descs ++ classDesc.listMethods()
                    .filter(_.method.getName == otherName))
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

    class MethodControl(val value: BasicInvocationRule,
                        val synchronizeReturnValue: Boolean = false,
                        val invokeOnly: Boolean = false,
                        val hide: Boolean = false,
                        @Nullable val procrastinator: Procrastinator = null) {

        private[SynchronizedObjectBehaviorBuilder] val paramMap = mutable.HashMap.empty[String, ParameterControl[Any]]

        def arg[P](argName: String)(configure: ParameterControl[P]): Unit = {
            paramMap.put(argName, configure.asInstanceOf[ParameterControl[Any]])
        }
    }

    class ParameterControl[P](val isActivated: Boolean) extends ParameterModifier[P]

    class FieldControl[A](val isActivated: Boolean) extends FieldModifier[A]

}
