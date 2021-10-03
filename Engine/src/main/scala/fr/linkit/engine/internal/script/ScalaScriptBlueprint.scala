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

import fr.linkit.api.internal.generation.compilation.access.CompilerType
import fr.linkit.api.internal.script.ScriptContext
import fr.linkit.engine.internal.generation.compilation.access.CommonCompilerTypes
import fr.linkit.engine.internal.language.cbp.AbstractClassBlueprint

import java.io.InputStream

class ScalaScriptBlueprint(bpIn: InputStream) extends AbstractClassBlueprint[ScriptContext](bpIn) {

    override val compilerType: CompilerType   = CommonCompilerTypes.Scalac
    override val rootScope   : RootValueScope = new RootValueScope {
        bindValue("ScriptCode" ~> (_.scriptSourceCode))
        bindValue("ScriptArguments" ~> (_.scriptArguments.map(p => s"${p._1}: ${p._2.getName}").mkString(", ")))
        bindValue("ScriptClass" ~> (_.scriptSuperClass.getName))
    }
}
