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

package fr.linkit.engine.gnom.persistence.config.profile.persistence

import fr.linkit.api.gnom.persistence.context.{ControlBox, Decomposition, ObjectTranform, TypePersistor}
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.config.structure.ClassObjectStructure
import fr.linkit.engine.internal.manipulation.creation.ObjectCreator
import fr.linkit.engine.internal.util.ScalaUtils

import java.lang.reflect.{Field, Modifier}

//a generic persistor for any kind of object (uses field scrapping)
final class GenericTypePersistor[T](override val structure: ClassObjectStructure) extends TypePersistor[T]() {

    private val fields: Array[Field] = structure.fields

    def this(clazz: Class[_]) = {
        this(ClassObjectStructure(clazz))
    }

    override def initInstance(instance: T, args: Array[Any], box: ControlBox): Unit = {
        ObjectCreator.pasteAllFields(instance, fields, args.asInstanceOf[Array[AnyRef]])
    }

    override def transform(t: T): ObjectTranform = {
        Decomposition(ObjectCreator.getAllFields(t, fields).asInstanceOf[Array[Any]])
    }
}
