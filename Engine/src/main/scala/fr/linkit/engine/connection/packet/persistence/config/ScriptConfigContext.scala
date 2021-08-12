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

import fr.linkit.engine.connection.packet.persistence.config.ScriptConfigContext.{BlacklistedLines, ScriptConfigName, ScriptConfigPackage}
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.script.SourceScriptContext

case class ScriptConfigContext(private val scriptCode: String,
                               scriptName: String,
                               override val parentLoader: ClassLoader = classOf[LinkitApplication].getClassLoader) extends SourceScriptContext(scriptName, parentLoader) {

    override lazy val scriptSourceCode: String = {
        val str = new StringBuilder(scriptCode)
        BlacklistedLines.foreach(line => {
            val idx = str.indexOf(line)
            str.delete(idx, idx + line.length + 2)
        })
        str.toString()
    }

    override def className: String = ScriptConfigName + scriptName

    override def classPackage: String = ScriptConfigPackage

}

object ScriptConfigContext {

    val ScriptConfigPackage = "gen.scala.script.config"
    val ScriptConfigName    = "ScalaConfig_"
    val BlacklistedLines    = Seq("import " + classOf[PersistenceConfigurationMethods].getName + "._")
}
