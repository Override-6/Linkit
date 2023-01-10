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

import fr.linkit.api.gnom.cache.sync.{ConnectedObjectReference, SynchronizedObject}
import fr.linkit.api.gnom.persistence.context.{ControlBox, Decomposition, ObjectTranform, Replaced, TypePersistor}
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.config.structure.SyncObjectStructure
import fr.linkit.engine.internal.util.ScalaUtils

class SynchronizedObjectPersistor[T <: SynchronizedObject[T]](objectPersistence: TypePersistor[T]) extends TypePersistor[T] {
    
    override val structure: ObjectStructure = new SyncObjectStructure(objectPersistence.structure)
    
    override def initInstance(syncObj: T, args: Array[Any], box: ControlBox): Unit = {
        setReference(syncObj, args)
        objectPersistence.initInstance(syncObj, args.dropRight(2), box)
    }
    
    private def setReference(syncObj: SynchronizedObject[T], args: Array[Any]): Unit = {
        val reference  : ConnectedObjectReference = args.last.asInstanceOf[ConnectedObjectReference]
        val isMirrored0: Boolean                  = args(args.length - 2).asInstanceOf[Boolean]
        val clazz: Class[_] = syncObj.getClass
        
        val field = clazz.getDeclaredField("location")
        ScalaUtils.setValue(syncObj, field, reference)
        val field1 = clazz.getDeclaredField("isMirrored0")
        ScalaUtils.setValue(syncObj, field1, isMirrored0)
    }
    
    override def transform(t: T): ObjectTranform = {
        val decomposed = (objectPersistence.transform(t) match {
            case Decomposition(decomposed) => decomposed
            case Replaced(replacement)     => Array(replacement)
        }) :+ t.isMirrored :+ t.reference
        Decomposition(decomposed)
    }
    
}
