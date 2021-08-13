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

package fr.linkit.engine.connection.packet.persistence.config

import fr.linkit.api.connection.packet.persistence.v3.PacketPersistenceContext
import fr.linkit.engine.connection.packet.persistence.config.ScriptConfigHandler.{ScriptName, ScriptPackage}
import fr.linkit.engine.local.script.{LinkitScriptHandler, ScriptContext}

class ScriptConfigHandler(context: PacketPersistenceContext) extends LinkitScriptHandler[PersistenceConfiguration] {

    override val scriptClassBlueprintResourcePath: String = "/generation/scala_config_script_file.scbp"

    override def newScript(clazz: Class[_ <: PersistenceConfiguration]): PersistenceConfiguration = {
        clazz.getConstructor(classOf[PacketPersistenceContext]).newInstance(context)
    }

    override protected val className   : String = ScriptName
    override protected val classPackage: String = ScriptPackage

    override def newScriptContext(scriptSourceCode: String, scriptName: String, scriptClassLoader: ClassLoader): ScriptContext = {
        new ScriptConfigContext(scriptSourceCode, scriptName, scriptClassLoader)
    }
}

object ScriptConfigHandler {

    val ScriptPackage = "gen.scala.script.persistence.config"
    val ScriptName    = "ScalaConfigScript_"
}
