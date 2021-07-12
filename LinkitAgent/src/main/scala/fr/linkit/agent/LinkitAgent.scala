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

package fr.linkit.agent

import java.lang.instrument.{ClassDefinition, Instrumentation}

object LinkitAgent {

    private var inst: Instrumentation = _

    def premain(agentArgs: String, inst: Instrumentation): Unit = {
        println(s"Linkit Agent started.")
        println(s"inst.isRedefineClassesSupported    = ${inst.isRedefineClassesSupported}")
        println(s"inst.isRetransformClassesSupported = ${inst.isRetransformClassesSupported}")
        this.inst = inst
    }

    def redefineClass(clazz: Class[_], bytecode: Array[Byte]): Unit = {
        inst.redefineClasses(new ClassDefinition(clazz, bytecode))
    }

    private def callerClass: Class[_] = {
        /*Thread.currentThread()
                .getStackTrace()(2)
                .getClassName*/
        //TODO faire en sorte que les méthodes statiques de l'agent ne soient utilisable que les classes directes de l'application
        ???
    }

}
