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

package fr.linkit.engine.gnom.persistence.defaults.special

import fr.linkit.api.gnom.persistence.context.{ControlBox, TypePersistence}
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.config.structure.ArrayObjectStructure
//a TP that does nothing (used to create Mirroring objects)
object EmptyObjectTypePersistence extends TypePersistence[AnyRef] {
    
    override val structure: ObjectStructure = ArrayObjectStructure()
    
    override def initInstance(allocatedObject: AnyRef, args: Array[Any], box: ControlBox): Unit = ()
    
    override def toArray(t: AnyRef): Array[Any] = Array()
}
