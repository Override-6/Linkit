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

package fr.linkit.engine.gnom.persistence.incident

import fr.linkit.api.gnom.persistence.incident.IncidentAlternative
import fr.linkit.engine.internal.mapping.{ClassMappings, MappedClassInfo}

case class UnknownObject private(args: Array[Any], classInfo: MappedClassInfo)

object UnknownObject {

    def apply(tpe: String, args: Array[Any]): IncidentAlternative[AnyRef] = () => {
        val info = ClassMappings.findUnknownClassInfo(tpe.hashCode).getOrElse {
            throw new NoSuchElementException(s"Could not find more unknown type information from class name '$tpe'.")
        }
        UnknownObject(args, info)
    }
}