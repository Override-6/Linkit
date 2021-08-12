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

package fr.linkit.api.local.generation

import fr.linkit.api.connection.cache.obj.description.{FieldDescription, MethodDescription}
import fr.linkit.api.local.generation.compilation.CompilationContext

import scala.reflect.runtime.universe.Type

trait PuppetClassDescription[A] extends CompilationContext {

    val classType: Type

    val clazz: Class[A]

    def listMethods(): Iterable[MethodDescription]

    def getMethodDescription(methodID: Int): Option[MethodDescription]

    def listFields(): Seq[FieldDescription]

    def getFieldDescription(fieldID: Int): Option[FieldDescription]
}
