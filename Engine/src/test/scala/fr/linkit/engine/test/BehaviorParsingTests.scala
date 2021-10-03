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

package fr.linkit.engine.test

import fr.linkit.engine.internal.language.bhv.TreeBehaviorParser
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Test, TestInstance}

@TestInstance(Lifecycle.PER_CLASS)
class BehaviorParsingTests {

    @Test
    def parseListBuffer(): Unit = {
        TreeBehaviorParser.parse(getClass.getResourceAsStream("/rmi_behaviors/ListBuffer.bhv"))
    }

}
