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

package fr.linkit.engine.internal.script

import fr.linkit.api.internal.generation.resource.ClassFolderResource
import fr.linkit.api.internal.script.{ScriptFile, ScriptHandler, ScriptInstantiator}
import fr.linkit.engine.internal.LinkitApplication

import java.io.InputStream

abstract class LinkitScriptHandler[S <: ScriptFile] extends ScriptHandler[S] {

    override def scriptClassBlueprint: InputStream = classOf[LinkitApplication].getResourceAsStream(scriptClassBlueprintResourcePath)

    override def findScript(scriptName: String, classes: ClassFolderResource[ScriptFile]): Option[ScriptInstantiator[S]] = {
        val scriptClassName = classPackage + '.' + className + scriptName
        classes.findClass[S](scriptClassName, classOf[LinkitApplication].getClassLoader).map { clazz =>
            newScript(clazz, _: _*)
        }
    }

    protected val className: String

    protected val classPackage: String

    protected val scriptClassBlueprintResourcePath: String

}
