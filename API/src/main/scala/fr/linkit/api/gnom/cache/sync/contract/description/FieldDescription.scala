/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.cache.sync.contract.description

import fr.linkit.api.gnom.cache.sync.contract.description.FieldDescription.computeID

import java.lang.reflect.Field

case class FieldDescription(javaField: Field, classDesc: SyncStructureDescription[_ <: AnyRef]) {

    val fieldId: Int = computeID(javaField)

    override def equals(obj: Any): Boolean = obj match {
        case d: FieldDescription => d.fieldId == fieldId && javaField == d.javaField && classDesc == d.classDesc
        case _                   => false
    }
}

object FieldDescription {

    def computeID(name: String, declaringClassName: String): Int = {
        name.hashCode + declaringClassName.hashCode
    }

    def computeID(field: Field): Int = computeID(field.getName, field.getDeclaringClass.getName)
}