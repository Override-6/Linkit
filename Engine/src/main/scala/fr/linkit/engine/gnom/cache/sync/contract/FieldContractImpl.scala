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

package fr.linkit.engine.gnom.cache.sync.contract

import fr.linkit.api.gnom.cache.sync.contract.description.FieldDescription
import fr.linkit.api.gnom.cache.sync.contract.level.ConcreteSyncLevel
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, SyncObjectFieldManipulation}
import fr.linkit.api.gnom.cache.sync.{ConnectedObject, SynchronizedObject}
import fr.linkit.engine.internal.util.ScalaUtils

class FieldContractImpl[A](val description: FieldDescription,
                           val autoChip   : Boolean,
                           val kind       : ConcreteSyncLevel) extends FieldContract[A] {

    private val isRegistered = kind != ConcreteSyncLevel.NotRegistered

    override def applyContract(obj: AnyRef with SynchronizedObject[AnyRef], manip: SyncObjectFieldManipulation): Unit = {
        val field      = description.javaField
        var fieldValue = ScalaUtils.getValue(obj, field)
        //As the given object is being synchronized,
        fieldValue = manip.findConnectedVersion(fieldValue).getOrElse(fieldValue)
        if (fieldValue == null) {
            ScalaUtils.setValue(obj, field, null)
            return
        }

        fieldValue = fieldValue match {
            case conn: ConnectedObject[AnyRef]      =>
                if (!conn.isInitialized) {
                    manip.initObject(conn)
                }
                conn
            case fieldValue: AnyRef if isRegistered => manip.createConnectedObject(fieldValue, kind)
            case _                                  => fieldValue
        }
        if (fieldValue != null && !field.getType.isAssignableFrom(fieldValue.getClass))
            throw new UnsupportedOperationException(s"Could not change value of field '$field' for object '$obj' to it's synchronized version : the original type of the field is not assignable with the new synchronized value's class (${fieldValue.getClass}).")
        ScalaUtils.setValue(obj, field, fieldValue)
    }
}
