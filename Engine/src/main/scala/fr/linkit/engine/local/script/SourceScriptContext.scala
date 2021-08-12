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

package fr.linkit.engine.local.script

import fr.linkit.engine.local.script.SimpleScriptHandler.{ScriptName, ScriptPackage}

abstract class SourceScriptContext(scriptName: String, private val loader: ClassLoader) extends ScriptContext {
    override val scriptSourceCode: String

    override def className: String = ScriptName + scriptName

    override def classPackage: String = ScriptPackage

    override def parentLoader: ClassLoader = loader

}

object SourceScriptContext {

    def apply(scriptCode: String, scriptName: String, loader: ClassLoader): SourceScriptContext = new SourceScriptContext(scriptName, loader) {
        override val scriptSourceCode: String = scriptCode
    }
}


