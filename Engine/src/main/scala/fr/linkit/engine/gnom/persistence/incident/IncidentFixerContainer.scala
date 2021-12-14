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

import fr.linkit.api.gnom.persistence.incident.fix.UnknownTypeFixer
import fr.linkit.engine.internal.mapping.MappedClassInfo

import scala.collection.mutable.ArrayBuffer

class IncidentFixerContainer {

    type Finder = MappedClassInfo => Boolean
    private val unknownTypeFixers = ArrayBuffer.empty[(Finder, UnknownTypeFixer)]

    def addUnknownTypeFixer(fixer: UnknownTypeFixer)(finder: Finder): Unit = {
        unknownTypeFixers += ((finder, fixer))
    }

    def getUnknownTypeFixer(classInfo: MappedClassInfo): UnknownTypeFixer = {
        for ((finder, fixer) <- unknownTypeFixers) {
            if (finder(classInfo))
                return fixer
        }
        throw new NoSuchElementException(s"Could not find any alternative that matches '${classInfo.className}'")
    }

}
