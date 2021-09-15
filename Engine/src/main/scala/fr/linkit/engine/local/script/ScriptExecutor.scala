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

import fr.linkit.api.local.ApplicationContext
import fr.linkit.api.local.generation.compilation.CompilerCenter
import fr.linkit.api.local.language.cbp.ClassBlueprint
import fr.linkit.api.local.script.{ScriptContext, ScriptFile, ScriptHandler, ScriptInstantiator}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.generation.compilation.factories.ClassCompilationRequestFactory
import fr.linkit.engine.local.generation.compilation.resource.CachedClassFolderResource
import fr.linkit.engine.local.resource.external.LocalResourceFolder

import java.net.URL
import scala.reflect.ClassTag

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
        val clazz       = center.processRequest {
            AppLogger.debug(s"Performing Class generation for script '$scriptName'")
            val blueprint = new ScalaScriptBlueprint(scriptHandler.scriptClassBlueprint).asInstanceOf[ClassBlueprint[ScriptContext]]
            new ClassCompilationRequestFactory[ScriptContext, S](blueprint)
                    .makeRequest(scriptHandler.newScriptContext(scriptCode, scriptName, additionalArguments, classLoader))
        }.getResult.get
        if (clazz == null)
            throw new ScriptCompileException(s"Some exception occurred during class compilation of script '$scriptName'. See above messages for further details.")
        scriptHandler.newScript(clazz, _)
    }

    @throws[ScriptCompileException]
    def newScalaScriptFromUrl[S <: ScriptFile](scriptUrl: URL, additionalArguments: Map[String, Class[_]])(implicit scriptHandler: ScriptHandler[S], center: CompilerCenter): ScriptInstantiator[S] = {
        val scriptCode = new String(scriptUrl.openStream().readAllBytes())
        val path       = scriptUrl.getPath
        val nameEndPos = path.lastIndexOf('.')
        val scriptName = path.slice(path.lastIndexOf('/') + 1, if (nameEndPos < 0) path.length else nameEndPos)
        newScalaScript[S](scriptName, additionalArguments)(scriptCode)(scriptHandler, center)
    }

    @throws[ScriptCompileException]
    def newScalaScriptFromUrl[S <: ScriptFile](scriptUrl: URL)(implicit scriptHandler: ScriptHandler[S], center: CompilerCenter): ScriptInstantiator[S] = {
        newScalaScriptFromUrl[S](scriptUrl, Map.empty[String, Class[_]])(scriptHandler, center)
    }

    @throws[ScriptCompileException]
    def getOrCreateScript[S <: ScriptFile](scriptUrl: URL, app: ApplicationContext, additionalArguments: Map[String, Class[_]] = Map.empty)(implicit scriptHandler: ScriptHandler[S]): ScriptInstantiator[S] = {
        val path       = scriptUrl.getPath
        val nameEndPos = path.lastIndexOf('.')
        val scriptName = path.slice(path.lastIndexOf('/') + 1, if (nameEndPos < 0) path.length else nameEndPos)
        getScript[S](scriptName, app).getOrElse(newScalaScriptFromUrl(scriptUrl, additionalArguments)(scriptHandler, app.compilerCenter))
    }

    @throws[ScriptCompileException]
    def getOrCreateScript(scriptUrl: URL, app: ApplicationContext): ScriptInstantiator[ScriptFile] = {
        getOrCreateScript[ScriptFile](scriptUrl, app)(DefaultScriptHandler)
    }

    def getScript(name: String)(implicit app: ApplicationContext): Option[ScriptInstantiator[ScriptFile]] = {
        getScript(name, app)(DefaultScriptHandler)
    }

    def getScript[S <: ScriptFile](name: String, app: ApplicationContext)(implicit handler: ScriptHandler[S]): Option[ScriptInstantiator[S]] = {
        val classTag  = ClassTag[CachedClassFolderResource[ScriptFile]](classOf[CachedClassFolderResource[ScriptFile]])
        val resources = app.getAppResources
                .getOrOpen[LocalResourceFolder](LinkitApplication.getProperty("compilation.working_dir.classes"))
                .getEntry
                .getOrAttachRepresentation[CachedClassFolderResource[ScriptFile]](classTag, CachedClassFolderResource.factory[ScriptFile])
        handler.findScript(name, resources)
    }

}