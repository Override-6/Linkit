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

import fr.linkit.api.connection.packet.traffic.PacketTraffic
import fr.linkit.api.local.script.{ScriptContext, ScriptFile}
import fr.linkit.engine.connection.packet.persistence.context.script.ScriptConfigContext.{DefaultScriptConfigParameter, EndOfIdeContext}
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.script.SimpleScriptHandler.{ScriptName, ScriptPackage}

case class ScriptConfigContext(private val scriptCode: String,
                               scriptName: String,
                               override val scriptSuperClass: Class[_ <: ScriptFile],
                               parameters: Map[String, Class[_]],
                               override val parentLoader: ClassLoader = classOf[LinkitApplication].getClassLoader)
        extends ScriptContext {

    override      val scriptArguments : Map[String, Class[_]] = parameters ++ DefaultScriptConfigParameter
    override lazy val scriptSourceCode: String                = {
        val builder        = new StringBuilder(scriptCode)
        val lines          = scriptCode.split('\n')
        val scriptStartIdx = lines.indexWhere(str => str.toLowerCase.replace("\\s+", "").startsWith(EndOfIdeContext))
        if (scriptStartIdx != -1) {
            var c = -1
            builder.dropWhile(char => (char == '\n') && {
                c += 1
                c
            } == scriptStartIdx)
        }
        builder.toString()
    }

    override def className: String = ScriptName + scriptName

    override def classPackage: String = ScriptPackage

}

object ScriptConfigContext {

    final val DefaultScriptConfigParameter = Map(
        "app" -> classOf[LinkitApplication],
        "traffic" -> classOf[PacketTraffic]
    )
    final val LineComment                  = "//"
    final val EndOfIdeContext              = "EndOfIDEContext".toLowerCase
}
