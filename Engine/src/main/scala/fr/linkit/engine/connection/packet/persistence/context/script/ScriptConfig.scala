/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *  
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *  
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.packet.persistence.context.script

import fr.linkit.api.connection.packet.persistence.context.{ObjectConverter, PersistenceConfig, PersistenceContext, TypePersistence}
import fr.linkit.api.local.script.ScriptFile
import fr.linkit.engine.connection.packet.persistence.context.PersistenceConfigBuilder

import scala.reflect.ClassTag

abstract class ScriptConfig extends PersistenceConfigBuilder with ScriptFile {

}

/**
 * This object should only be used for it's method declarations in order to add some context for IDEs 
 * into Script Files.
 * @see [[ScriptConfigContext]]
 * */
object ScriptConfig extends ScriptConfig {
    private final val Msg = "This object should only be used for it's method declarations in order to add some context for IDEs into Script Files."

    override def putContextReference(ref: AnyRef): Unit = {
        fail()
    }

    override def putContextReference(id: Int, ref: AnyRef): Unit = {
        fail()
    }

    override def addPersistence[T <: AnyRef : ClassTag](persistence: TypePersistence[T]): ScriptConfig.this.type = {
        fail()
    }

    override def setTConverter[T <: AnyRef : ClassTag, B](converter: ObjectConverter[T, B]): ScriptConfig.this.type = {
        fail()
    }

    override def setTNewConverter[T <: AnyRef : ClassTag, B](fTo: T => B)(fFrom: B => T): ScriptConfig.this.type = {
        fail()
    }

    override def build(context: PersistenceContext): PersistenceConfig = {
        fail()
    }

    override def execute(): Unit = {
        fail()
    }

    def fail(): Nothing = throw new UnsupportedOperationException(Msg)

}
