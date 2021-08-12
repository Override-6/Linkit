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

class SimpleScriptHandler extends LinkitScriptHandler[ScriptFile] {

    override val scriptClassBlueprintResourcePath: String = "/generation/scala_script_file.scbp"

    override def newScript(clazz: Class[_ <: ScriptFile]): ScriptFile = clazz.getConstructor().newInstance()

    override protected val className   : String = ScriptName
    override protected val classPackage: String = ScriptPackage

    override def newScriptContext(scriptSourceCode: String, scriptName: String, scriptClassLoader: ClassLoader): ScriptContext = {
        SourceScriptContext(scriptSourceCode, scriptName, scriptClassLoader)
    }
}

object SimpleScriptHandler {

    val ScriptPackage = "gen.scala.script"
    val ScriptName    = "ScalaScript_"
}
