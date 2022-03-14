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

package fr.linkit.engine.gnom.cache.sync.contract

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.description.FieldDescription
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, SyncObjectFieldManipulation}
import fr.linkit.engine.internal.utils.ScalaUtils

class FieldContractImpl[A](val desc: FieldDescription,
                           val isSynchronized: Boolean) extends FieldContract[A] {

    override def applyContract(obj: AnyRef with SynchronizedObject[AnyRef], manip: SyncObjectFieldManipulation): Unit = {
        val field      = desc.javaField
        var fieldValue = ScalaUtils.getValue(obj, field)
        //As the given object is being synchronized,
        fieldValue = manip.findSynchronizedVersion(fieldValue).getOrElse(fieldValue)
        if (isSynchronized && fieldValue != null) {
            fieldValue = fieldValue match {
                case sync: SynchronizedObject[AnyRef] =>
                    if (!sync.isInitialized) {
                        manip.initSyncObject(sync)
                    }
                    sync
                case fieldValue: AnyRef               =>
                    manip.syncObject(fieldValue)
            }
        }
        ScalaUtils.setValue(obj, field, fieldValue)
    }
}
