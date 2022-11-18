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

package fr.linkit.engine.gnom.persistence.defaults.special

import fr.linkit.api.gnom.persistence.context.{ControlBox, Decomposition, ObjectTranform, TypePersistor}
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.config.structure.EmptyObjectStructure

//a TP that does nothing (used to create Mirroring objects)
object EmptyObjectTypePersistor$ extends TypePersistor[AnyRef] {

    override val structure: ObjectStructure = EmptyObjectStructure

    override def initInstance(allocatedObject: AnyRef, args: Array[Any], box: ControlBox): Unit = ()

    override def transform(t: AnyRef): ObjectTranform = Decomposition(Array())
}
