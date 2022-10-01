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

package fr.linkit.engine.internal.script

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.resource.local.LocalFolder
import fr.linkit.api.internal.compilation.CompilerCenter
import fr.linkit.api.internal.language.cbp.ClassBlueprint
import fr.linkit.api.internal.script.{ScriptContext, ScriptFile, ScriptHandler, ScriptInstantiator}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.internal.compilation.ClassCompilationRequestFactory
import fr.linkit.engine.internal.compilation.resource.CachedClassFolderResource

import java.net.URL

object ScriptExecutor {

    val DefaultScriptHandler = new SimpleScriptHandler[ScriptFile]

    @throws[ScriptCompileException]
    def newScalaScript(scriptName: String)(scriptCode: String)(implicit center: CompilerCenter): ScriptInstantiator[ScriptFile] = {
        newScalaScript[ScriptFile](scriptName, Map.empty)(scriptCode)(DefaultScriptHandler, center)
    }

    @throws[ScriptCompileException]
    def newScalaScript(scriptName: String, additionalArguments: Map[String, Class[_]])(scriptCode: String)(implicit center: CompilerCenter): ScriptInstantiator[ScriptFile] = {
        newScalaScript[ScriptFile](scriptName, additionalArguments)(scriptCode)(DefaultScriptHandler, center)
    }

    @throws[ScriptCompileException]
    def newScalaScript[S <: ScriptFile](scriptName: String, additionalArguments: Map[String, Class[_]] = Map.empty)(scriptCode: String)(implicit scriptHandler: ScriptHandler[S], center: CompilerCenter): ScriptInstantiator[S] = {
        val classLoader = Thread.currentThread().getContextClassLoader
        val result      = center.processRequest {
            AppLoggers.Compilation.info(s"Performing Class generation for script '$scriptName'")
            val blueprint = new ScalaScriptBlueprint(scriptHandler.scriptClassBlueprint).asInstanceOf[ClassBlueprint[ScriptContext]]
            new ClassCompilationRequestFactory[ScriptContext, S](blueprint)
                .makeRequest(scriptHandler.newScriptContext(scriptCode, scriptName, additionalArguments, classLoader))
        }
        AppLoggers.Compilation.info(s"Script '$scriptName' class generation ended in ${result.getCompileTime} ms.")
        val classes = result.getClasses
        if (classes.isEmpty)
            throw new ScriptCompileException(s"Some exception occurred during class compilation of script '$scriptName'. See above messages for further details.")
        scriptHandler.newScript(classes.head, _: _*)
    }

    private def toScriptName(scriptUrl: URL): String = {
        val path       = scriptUrl.getPath
        val nameEndPos = path.lastIndexOf('.')
        path.slice(path.lastIndexOf('/') + 1, if (nameEndPos < 0) path.length else nameEndPos)
    }

    @throws[ScriptCompileException]
    def newScalaScriptFromUrl[S <: ScriptFile](scriptUrl: URL, additionalArguments: Map[String, Class[_]])(implicit scriptHandler: ScriptHandler[S], center: CompilerCenter): ScriptInstantiator[S] = {
        val scriptCode = new String(scriptUrl.openStream().readAllBytes())
        newScalaScript[S](toScriptName(scriptUrl), additionalArguments)(scriptCode)(scriptHandler, center)
    }

    @throws[ScriptCompileException]
    def newScalaScriptFromUrl[S <: ScriptFile](scriptUrl: URL)(implicit scriptHandler: ScriptHandler[S], center: CompilerCenter): ScriptInstantiator[S] = {
        newScalaScriptFromUrl[S](scriptUrl, Map.empty[String, Class[_]])(scriptHandler, center)
    }

    @throws[ScriptCompileException]
    def getOrCreateScript[S <: ScriptFile](scriptUrl: URL, app: ApplicationContext, additionalArguments: Map[String, Class[_]] = Map.empty)(implicit scriptHandler: ScriptHandler[S]): ScriptInstantiator[S] = {
        findScript[S](toScriptName(scriptUrl), app).getOrElse(newScalaScriptFromUrl(scriptUrl, additionalArguments)(scriptHandler, app.compilerCenter))
    }

    @throws[ScriptCompileException]
    def getOrCreateScript(scriptUrl: URL, app: ApplicationContext): ScriptInstantiator[ScriptFile] = {
        getOrCreateScript[ScriptFile](scriptUrl, app)(DefaultScriptHandler)
    }

    def findScript(name: String)(implicit app: ApplicationContext): Option[ScriptInstantiator[ScriptFile]] = {
        findScript(name, app)(DefaultScriptHandler)
    }

    def findScript[S <: ScriptFile](name: String, app: ApplicationContext)(implicit handler: ScriptHandler[S]): Option[ScriptInstantiator[S]] = {
        val resources = app.getAppResources
                           .getOrOpen[LocalFolder](LinkitApplication.getProperty("compilation.working_dir") + "/Classes")
                           .getEntry
                           .getOrAttachRepresentation[CachedClassFolderResource[ScriptFile]]("script")
        handler.findScript(name, resources)
    }

}