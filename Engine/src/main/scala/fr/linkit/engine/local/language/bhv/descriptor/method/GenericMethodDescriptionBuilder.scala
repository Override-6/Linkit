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

package fr.linkit.engine.local.language.bhv.descriptor.method

import fr.linkit.engine.local.language.bhv.descriptor.clazz.ClassDescriptionResultBuilder

import java.util.Scanner

class GenericMethodDescriptionBuilder(scanner: Scanner, classBuilder: ClassDescriptionResultBuilder) extends AbstractMethodDescriptionResultBuilder(scanner, classBuilder) {
    launchParsing()

    override protected def parseCategory(name: String): Unit = {
        throw new MethodBehaviorDescriptionException(s"Unknown category '$name' into generic method descriptor.")
    }

    override def result(): MethodBehaviorDescriptionResult = {
        new MethodBehaviorDescriptionResult(true, null, syncReturnValue, behaviorRule)
    }

    override def baseCurrentOn(result: MethodBehaviorDescriptionResult): Unit = throw new MethodBehaviorDescriptionException(s"Could not base generic method description on another result.")
}
