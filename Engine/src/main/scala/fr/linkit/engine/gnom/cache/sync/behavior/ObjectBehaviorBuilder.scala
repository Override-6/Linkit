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

package fr.linkit.engine.gnom.cache.sync.behavior

import fr.linkit.api.gnom.cache.sync.behavior.ObjectBehavior
import fr.linkit.api.gnom.cache.sync.behavior.annotation.BasicInvocationRule
import fr.linkit.api.gnom.cache.sync.behavior.member.MemberBehaviorFactory
import fr.linkit.api.gnom.cache.sync.behavior.member.field.{FieldBehavior, FieldModifier}
import fr.linkit.api.gnom.cache.sync.behavior.member.method.InternalMethodBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.parameter.{ParameterBehavior, ParameterModifier}
import fr.linkit.api.gnom.cache.sync.behavior.member.method.returnvalue.ReturnValueModifier
import fr.linkit.api.gnom.cache.sync.description.{FieldDescription, MethodDescription, SyncObjectSuperclassDescription}
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.behavior.ObjectBehaviorBuilder.{FieldControl, MethodControl}
import fr.linkit.engine.gnom.cache.sync.behavior.member.{MethodParameterBehavior, SyncFieldBehavior, SyncMethodBehavior}
import fr.linkit.engine.gnom.cache.sync.description.SimpleSyncObjectSuperClassDescription
import fr.linkit.engine.gnom.cache.sync.invokation.DefaultMethodInvocationHandler
import org.jetbrains.annotations.Nullable

import java.util
import scala.collection.mutable
import scala.reflect.ClassTag

abstract class ObjectBehaviorBuilder[T <: AnyRef] private(val classDesc: SyncObjectSuperclassDescription[T]) {

    protected val methodsMap                            = mutable.HashMap.empty[MethodDescription, MethodControl]
    protected val fieldsMap                             = mutable.HashMap.empty[FieldDescription, FieldControl[AnyRef]]
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
                    .filter(_.javaMethod.getName == name)
        )
    }

    final def annotateAllMethods: MethodModification = {
        new MethodModification(classDesc.listMethods())
    }

    def build(factory: MemberBehaviorFactory): ObjectBehavior[T] = {
        new DefaultObjectBehavior[T](classDesc, factory, Option(asField), Option(asParameter), Option(asReturnValue)) {
            override protected def generateMethodsBehavior(): Iterable[InternalMethodBehavior] = {
                classDesc.listMethods()
                        .map(genMethodBehavior(factory, _))
            }

            override protected def generateFieldsBehavior(): Iterable[FieldBehavior[AnyRef]] = {
                classDesc.listFields()
                        .map(genFieldBehavior(factory, _))
            }
        }
    }

    private def genFieldBehavior(factory: MemberBehaviorFactory, desc: FieldDescription): FieldBehavior[AnyRef] = {
        val opt = fieldsMap.get(desc)
        if (opt.isEmpty)
            factory.genFieldBehavior(desc)
        else {
            val control = opt.get
            SyncFieldBehavior[AnyRef](desc, control.isActivated, control)
        }
    }

    private def genMethodBehavior(factory: MemberBehaviorFactory, desc: MethodDescription): InternalMethodBehavior = {
        val opt = methodsMap.get(desc)
        if (opt.isEmpty)
            factory.genMethodBehavior(None, desc)
        else {
            val control = opt.get
            import control._
            val handler = DefaultMethodInvocationHandler
            val params  = extractParams(control, desc)
            SyncMethodBehavior(desc, params, synchronizeReturnValue, hide, innerInvocations, Array(value), procrastinator, handler)
        }
    }

    private def extractParams(control: MethodControl, desc: MethodDescription): Array[ParameterBehavior[AnyRef]] = {
        val javaMethod = desc.javaMethod
        val defaults   = AnnotationBasedMemberBehaviorFactory.getSynchronizedParams(javaMethod)
        control.paramNameMap.foreachEntry((argName, argControl) => {
            val i = defaults.indexWhere(_.getName == argName)
            if (i > 0) defaults(i) = new MethodParameterBehavior[AnyRef](defaults(i).param, argControl.isActivated, argControl)
            else throw new IllegalArgumentException(s"Unknown parameter '$argName' for method ${javaMethod.getName}")
        })
        control.paramPosMap.foreachEntry((argIdx, argControl) => {
            defaults(argIdx) = new MethodParameterBehavior[AnyRef](defaults(argIdx).param, argControl.isActivated, argControl)
        })
        defaults
    }

    class MethodModification private[ObjectBehaviorBuilder](descs: Iterable[MethodDescription]) {

        def by(control: MethodControl): this.type = {
            descs.foreach(methodsMap.put(_, control))
            this
        }

        def and(otherName: String): MethodModification = {
            new MethodModification(descs ++ classDesc.listMethods()
                    .filter(_.javaMethod.getName == otherName))
        }
    }

    class FieldModification private[ObjectBehaviorBuilder](descs: Iterable[FieldDescription]) {

        def by(control: FieldControl[Any]): this.type = {
            descs.foreach(fieldsMap.put(_, control.asInstanceOf[FieldControl[AnyRef]]))
            this
        }

        def and(otherName: String): FieldModification = {
            new FieldModification(descs ++ classDesc.listFields()
                    .filter(_.javaField.getName == otherName))
        }
    }

}

object ObjectBehaviorBuilder {

    class MethodControl(val value: BasicInvocationRule,
                        val synchronizeReturnValue: Boolean = false,
                        val hide: Boolean = false,
                        val innerInvocations: Boolean = false,
                        @Nullable val procrastinator: Procrastinator = null) {

        private[ObjectBehaviorBuilder] val paramNameMap = mutable.HashMap.empty[String, ParameterControl[AnyRef]]
        private[ObjectBehaviorBuilder] val paramPosMap = mutable.HashMap.empty[Int, ParameterControl[AnyRef]]

        def arg[P](argName: String)(configure: ParameterControl[P]): Unit = {
            paramNameMap.put(argName, configure.asInstanceOf[ParameterControl[AnyRef]])
        }

        def arg[P](index: Int)(configure: ParameterControl[P]): Unit = {
            paramPosMap.put(index, configure.asInstanceOf[ParameterControl[AnyRef]])
        }
    }

    class ParameterControl[P](val isActivated: Boolean) extends ParameterModifier[P]

    class FieldControl[A](val isActivated: Boolean) extends FieldModifier[A]

}
