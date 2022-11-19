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

import fr.linkit.api.internal.compilation.ClassFolderResource
import fr.linkit.api.internal.script.{ScriptFile, ScriptHandler, ScriptInstantiator}
import java.io.InputStream

import fr.linkit.engine.application.LinkitApplication

abstract class AbstractScriptHandler[S <: ScriptFile] extends ScriptHandler[S] {

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
