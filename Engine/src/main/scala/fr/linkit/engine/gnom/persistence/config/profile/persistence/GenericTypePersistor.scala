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
import fr.linkit.engine.gnom.persistence.config.profile.persistence.GenericTypePersistor.{GenericObjectStructure, scrapFields}
import fr.linkit.engine.internal.manipulation.creation.ObjectCreator
import fr.linkit.engine.internal.util.ScalaUtils

import java.lang.reflect.{Field, Modifier}
import java.util

//a generic persistor for any kind of object (uses field scrapping)
final class GenericTypePersistor[T](baseClass: Class[_]) extends TypePersistor[T]() {


    override val structure                   = GenericObjectStructure
    private  val defaultFields: Array[Field] = scrapFields(baseClass)

    private val subClassFields = new util.HashMap[Class[_ <: T], Array[Field]]

    private def getFields(clazz: Class[_ <: T]): Array[Field] = {
        var fields = subClassFields.get(clazz)
        if (fields == null) {
            fields = scrapFields(clazz)
            subClassFields.put(clazz, fields)
        }
        fields
    }

    override def initInstance(instance: T, args: Array[Any], box: ControlBox): Unit = {
        val clazz  = instance.getClass
        val fields = if (clazz eq baseClass) defaultFields else getFields(clazz)
        ObjectCreator.pasteAllFields(instance, fields, args.asInstanceOf[Array[AnyRef]])
    }

    override def transform(t: T): ObjectTranform = {
        val clazz  = t.getClass
        val fields = if (clazz eq baseClass) defaultFields else getFields(clazz)
        Decomposition(ObjectCreator.getAllFields(t, fields).asInstanceOf[Array[Any]])
    }
}

object GenericTypePersistor {

    private def scrapFields(clazz: Class[_]): Array[Field] = {
        ScalaUtils.retrieveAllFields(clazz).filterNot(f => Modifier.isTransient(f.getModifiers))
    }

    object GenericObjectStructure extends ObjectStructure {
        override def isAssignable(args: Array[Class[_]], from: Int, to: Int): Boolean = true

        override def isAssignable(args: Array[Any], from: Int, to: Int): Boolean = true
    }
}