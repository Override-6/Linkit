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

import fr.linkit.api.connection.cache.obj.behavior.annotation.BasicRemoteInvocationRule
import fr.linkit.api.connection.cache.obj.behavior.{FieldBehavior, MemberBehaviorFactory, MethodBehavior, WrapperBehavior}
import fr.linkit.api.connection.cache.obj.description.MethodDescription
import fr.linkit.api.local.generation.PuppetClassDescription
import fr.linkit.engine.connection.cache.obj.behavior.WrapperBehaviorBuilder.MethodControl
import fr.linkit.engine.connection.cache.obj.description.SimpleClassDescription
import fr.linkit.engine.connection.cache.obj.invokation.local.{DefaultRMIHandler, InvokeOnlyRMIHandler}

import java.util
import scala.collection.mutable
import scala.reflect.ClassTag

class WrapperBehaviorBuilder[T] private(val classDesc: PuppetClassDescription[T]) {

    val methodsMap = mutable.HashMap.empty[MethodDescription, MethodControl]

    def this()(implicit classTag: ClassTag[T]) = {
        this(SimpleClassDescription(classTag.runtimeClass.asInstanceOf[Class[T]]))
    }

    final def annotateMethod(name: String, params: Class[_]*): MethodModification = {
        val methodID = name.hashCode + util.Arrays.hashCode(params.toArray[AnyRef])
        val method   = classDesc.getMethodDescription(methodID).getOrElse(throw new NoSuchElementException(s"Method description '$name' not found."))
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

    def build(factory: MemberBehaviorFactory): WrapperBehavior[T] = {
        new WrapperInstanceBehavior[T](classDesc, factory) {
            override protected def generateMethodsBehavior(): Iterable[MethodBehavior] = {
                classDesc.listMethods()
                        .map(genMethodBehavior(factory, _))
            }

            override protected def generateFieldsBehavior(): Iterable[FieldBehavior] = {
                //TODO Handle field customisation as well
                super.generateFieldsBehavior()
            }
        }
    }

    private def genMethodBehavior(factory: MemberBehaviorFactory, desc: MethodDescription): MethodBehavior = {
        val opt = methodsMap.get(desc)
        if (opt.isEmpty)
            factory.genMethodBehavior(desc)
        else {
            val control = opt.get
            import control._
            val handler = if (invokeOnly) InvokeOnlyRMIHandler else DefaultRMIHandler
            new MethodBehavior(desc, synchronizedParams, synchronizeReturnValue, hide, Array(value), handler)
        }
    }

    class MethodModification private[WrapperBehaviorBuilder](descs: Iterable[MethodDescription]) {

        def by(control: MethodControl): this.type = {
            descs.foreach(methodsMap.put(_, control))
            this
        }

        def and(otherName: String): MethodModification = {
            new MethodModification(descs ++ classDesc.listMethods()
                    .filter(_.symbol.name.toString == otherName))
        }
    }
}

object WrapperBehaviorBuilder {

    case class MethodControl(value: BasicRemoteInvocationRule, synchronizeReturnValue: Boolean = false, invokeOnly: Boolean = false, hide: Boolean = false, synchronizedParams: Seq[Boolean] = null)

}
