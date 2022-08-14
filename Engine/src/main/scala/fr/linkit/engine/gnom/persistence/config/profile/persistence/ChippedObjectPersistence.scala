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

import fr.linkit.api.gnom.cache.sync.{ChippedObject, ConnectedObjectReference}
import fr.linkit.api.gnom.persistence.context.{ControlBox, TypePersistence}
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.config.structure.SyncObjectStructure
import fr.linkit.engine.internal.util.{JavaUtils, ScalaUtils}
import org.jetbrains.annotations.Nullable

class ChippedObjectPersistence(@Nullable chi: ChippedObject[_], objectPersistence: TypePersistence[AnyRef]) extends TypePersistence[AnyRef] {
    
    override val structure: ObjectStructure = new SyncObjectStructure(objectPersistence.structure)
    
    override def toArray(t: AnyRef): Array[Any] = {
        if (!JavaUtils.sameInstance(t, chi.connected))
            throw new IllegalArgumentException("toArray argument value is not chipped origin of chi field value.")
        objectPersistence.toArray(t) :+ chi.isMirrored :+ chi.reference
    }
    
    override def initInstance(obj: AnyRef, args: Array[Any], box: ControlBox): Unit = {
        obj match {
            case chi: ChippedObject[_] =>
                setReference(chi, args)
                objectPersistence.initInstance(chi, args.dropRight(2), box)
            case _                     =>
                throw new IllegalArgumentException(s"$obj is not an instance of ChippedObject.")
            
        }
    }
    
    private def setReference(syncObj: ChippedObject[_], args: Array[Any]): Unit = {
        val obj                                   = syncObj.connected
        val reference  : ConnectedObjectReference = args.last.asInstanceOf[ConnectedObjectReference]
        val isMirrored0: Boolean                  = args(args.length - 2).asInstanceOf[Boolean]
        val clazz      : Class[_]                 = obj.getClass
        
        val field = clazz.getDeclaredField("location")
        ScalaUtils.setValue(obj, field, reference)
        val field1 = clazz.getDeclaredField("isMirrored0")
        ScalaUtils.setValue(obj, field1, isMirrored0)
    }
}