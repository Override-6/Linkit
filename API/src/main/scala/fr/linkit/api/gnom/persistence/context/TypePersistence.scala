/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.gnom.persistence.context

import fr.linkit.api.gnom.persistence.obj.ObjectStructure

trait TypePersistence[-T] {

    val structure: ObjectStructure

    def initInstance(allocatedObject: T, args: Array[Any]): Unit

    def toArray(t: T): Array[Any]
}
