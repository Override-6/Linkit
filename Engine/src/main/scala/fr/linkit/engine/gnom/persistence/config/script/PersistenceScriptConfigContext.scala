/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.persistence.config.script

import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.internal.script.{ScriptContext, ScriptFile}
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.gnom.persistence.config.script.PersistenceScriptConfigContext.{DefaultScriptConfigParameter, EndOfContext, LineComment, StartOfContext}
import fr.linkit.engine.internal.script.ScriptException

import scala.collection.mutable

case class PersistenceScriptConfigContext(private val scriptCode: String,
                                          scriptName: String,
                                          override val scriptSuperClass: Class[_ <: ScriptFile],
                                          parameters: Map[String, Class[_]],
                                          override val parentLoader: ClassLoader = classOf[LinkitApplication].getClassLoader)
        extends ScriptContext {

    override      val scriptArguments : Map[String, Class[_]] = parameters ++ DefaultScriptConfigParameter
    override lazy val scriptSourceCode: String                = {
        val builder = new mutable.StringBuilder(scriptCode)
        val lines   = scriptCode.split('\n')

        def idxOfLineStartThatContains(searched: String): Int = {
            var total = 0
            for (str <- lines) {
                total += str.length
                val formattedLine = str.toLowerCase.replace(" ", "")
                if (formattedLine.startsWith(searched))
                    return total
            }
            -1
        }

        val contextStartIdx = idxOfLineStartThatContains(LineComment + StartOfContext)
        val contextEndIdx   = idxOfLineStartThatContains(LineComment + EndOfContext)
        if (contextStartIdx != -1 && contextEndIdx != -1) {

            if (contextStartIdx > contextEndIdx) {
                throw new ScriptException("Configuration script 'Ide context comment controllers' are illogical : ide context start comment is after ide context end comment.")
            }
            builder.delete(contextStartIdx, contextEndIdx)
        }
        builder.toString()
    }

    override def className: String = {
        ScriptPersistenceConfigHandler.className + scriptName
    }

    override def classPackage: String = {
        ScriptPersistenceConfigHandler.classPackage
    }

}

object PersistenceScriptConfigContext {

    final val DefaultScriptConfigParameter = Map(
        "app" -> classOf[LinkitApplication],
        "traffic" -> classOf[PacketTraffic]
    )
    final val LineComment                  = "//"
    final val EndOfContext                 = "EndOfContext".toLowerCase
    final val StartOfContext               = "StartOfContext".toLowerCase
}
