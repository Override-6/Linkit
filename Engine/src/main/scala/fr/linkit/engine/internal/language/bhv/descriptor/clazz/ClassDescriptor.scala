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

package fr.linkit.engine.internal.language.bhv.descriptor.clazz

import fr.linkit.engine.gnom.cache.sync.description.SyncObjectDescription
import fr.linkit.engine.internal.language.bhv.descriptor.{DescriptionResult, Descriptor}

import java.util.Scanner

class ClassDescriptor(desc: SyncObjectDescription[_]) extends Descriptor {

    def this(clazz: Class[_]) = this(SyncObjectDescription(clazz))

    private val methods = desc.listMethods()
    private val fields  = desc.listFields()

    override def describe(scanner: Scanner): DescriptionResult = {
        val builder = new ClassDescriptionResultBuilder(scanner, desc)
        builder.result()
    }

}





















