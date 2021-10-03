/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.internal.language.bhv.descriptor.method

import fr.linkit.api.gnom.cache.sync.description.MethodDescription
import fr.linkit.engine.internal.language.bhv.descriptor.Descriptor
import fr.linkit.engine.internal.language.bhv.descriptor.clazz.ClassDescriptionResultBuilder
import org.jetbrains.annotations.Nullable

import java.util.Scanner

class MethodDescriptor(@Nullable methodDesc: MethodDescription, classBuilder: ClassDescriptionResultBuilder) extends Descriptor {



    override def describe(scanner: Scanner): MethodBehaviorDescriptionResult = {
        val word = scanner.next()
        if (word != "as") //must start with "as"
            throw new MethodBehaviorDescriptionException(s"Method descriptor must start with 'as'. found: $word")
        val builder = if (methodDesc != null) new ExplicitMethodDescriptionResultBuilder(methodDesc, classBuilder, scanner) else new GenericMethodDescriptionBuilder(scanner, classBuilder)
        builder.result()
    }
}
