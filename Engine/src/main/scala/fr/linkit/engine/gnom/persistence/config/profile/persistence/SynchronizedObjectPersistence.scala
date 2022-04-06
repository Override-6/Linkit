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

package fr.linkit.engine.gnom.persistence.config.profile.persistence

import fr.linkit.api.gnom.cache.sync.{SyncObjectReference, SynchronizedObject}
import fr.linkit.api.gnom.persistence.context.{ControlBox, TypePersistence}
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.config.structure.SyncObjectStructure
import fr.linkit.engine.internal.utils.ScalaUtils

class SynchronizedObjectPersistence[T <: SynchronizedObject[T]](objectPersistence: TypePersistence[T]) extends TypePersistence[T] {

    override val structure: ObjectStructure = new SyncObjectStructure(objectPersistence.structure)

    override def initInstance(syncObj: T, args: Array[Any], box: ControlBox): Unit = {
        setReference(syncObj, args.last.asInstanceOf[SyncObjectReference])
        objectPersistence.initInstance(syncObj, args.dropRight(1), box)
    }

    private def setReference(syncObj: SynchronizedObject[T], reference: SyncObjectReference): Unit = {
        val field = syncObj.getClass.getDeclaredField("location")
        ScalaUtils.setValue(syncObj, field, reference)
    }

    override def toArray(t: T): Array[Any] = {
        (objectPersistence.toArray(t): Array[Any]) :+ (t.reference: Any)
    }

}
