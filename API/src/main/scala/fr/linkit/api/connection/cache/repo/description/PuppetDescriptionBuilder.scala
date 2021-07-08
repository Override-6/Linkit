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

package fr.linkit.api.connection.cache.repo.description

import fr.linkit.api.connection.cache.repo.annotations.InvocationKind
import fr.linkit.api.connection.cache.repo.description.PuppetDescription.MethodDescription
import fr.linkit.api.connection.cache.repo.description.PuppetDescriptionBuilder.MethodControl

import java.util
import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.universe._

class PuppetDescriptionBuilder[T](desc: PuppetDescription[T]) {

    def this(clazz: Class[T]) {
        this(PuppetDescription(clazz))
    }

    final def annotate(name: String, params: Class[_]*): MethodModification = {
        val methodID = name.hashCode + util.Arrays.hashCode(params.toArray[AnyRef])
        val method   = desc.getMethodDesc(methodID).getOrElse(throw new NoSuchElementException(s"Method description '$name' not found."))
        new MethodModification(method)
    }

    final def annotateAll(name: String): MethodModification = {
        new MethodModification(
            desc.listMethods()
                    .filter(_.symbol.name.toString == name): _*
        )
    }

    class MethodModification private[PuppetDescriptionBuilder](descs: MethodDescription*) {

        def by(control: MethodControl): Unit = descs.foreach { desc =>
            desc.invocationKind = control.value
            desc.synchronizedParams = PuppetDescription.toSyncParamsIndexes(control.mutates, desc.symbol)
            desc.isPure = control.pure
            desc.syncReturnValue = control.synchronizeReturnValue
            desc.isHidden = control.hide
        }
    }

    def result: PuppetDescription[T] = desc

}

object PuppetDescriptionBuilder {

    case class MethodControl(value: InvocationKind, pure: Boolean = false, mutates: String = "", synchronizeReturnValue: Boolean = false, hide: Boolean = false) {

    }

}
