/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.script

import fr.linkit.api.internal.script.{ScriptContext, ScriptFile}
import fr.linkit.engine.internal.script.SimpleScriptHandler.{CBResourcePath, ScriptName, ScriptPackage}

class SimpleScriptHandler[S <: ScriptFile] extends LinkitScriptHandler[S] {

    override val scriptClassBlueprintResourcePath: String = CBResourcePath

    override def newScript(clazz: Class[_ <: S], args: Any*): S = {
        clazz.getDeclaredConstructors.head.newInstance(args: _*)
                .asInstanceOf[S]
    }

    override protected val className   : String = ScriptName
    override protected val classPackage: String = ScriptPackage

    override def newScriptContext(scriptSourceCode: String, scriptName: String, additionalArguments: Map[String, Class[_]], scriptClassLoader: ClassLoader): ScriptContext = {
        new ScriptContext {
            override val scriptSourceCode: String                 = scriptSourceCode
            override val scriptArguments : Map[String, Class[_]]  = additionalArguments
            override val scriptSuperClass: Class[_ <: ScriptFile] = classOf[ScriptFile]

            override def className: String = ScriptName

            override def classPackage: String = ScriptPackage

            override def parentLoader: ClassLoader = scriptClassLoader
        }
    }
}

object SimpleScriptHandler {

    final val CBResourcePath = "/generation/scala_script_file.scbp"
    final val ScriptPackage  = "gen.scala.script"
    final val ScriptName     = "ScalaScript"
}
