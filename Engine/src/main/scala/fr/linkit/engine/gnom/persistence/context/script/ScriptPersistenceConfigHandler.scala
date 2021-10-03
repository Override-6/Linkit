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

package fr.linkit.engine.gnom.persistence.context.script

import fr.linkit.api.internal.script.ScriptContext
import fr.linkit.engine.internal.script.SimpleScriptHandler
import fr.linkit.engine.internal.script.SimpleScriptHandler.{ScriptName, ScriptPackage}

object ScriptPersistenceConfigHandler extends SimpleScriptHandler[PersistenceScriptConfig] {
    override val className   : String = "ScalaPersistenceConfigScript_"
    override val classPackage: String = "gen.scala.persistence.config.script"

    override def newScriptContext(scriptSourceCode: String, scriptName: String, additionalArguments: Map[String, Class[_]], scriptClassLoader: ClassLoader): ScriptContext = {
        new ScriptConfigContext(scriptSourceCode, scriptName, classOf[PersistenceScriptConfig], additionalArguments, scriptClassLoader)
    }
}