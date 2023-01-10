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

package linkit.base.script

import linkit.base.compilation.access.CommonCompilerType
import linkit.base.compilation.cbp.AbstractClassBlueprint.RootValueScope

import java.io.InputStream

class ScalaScriptBlueprint(bpIn: InputStream) extends AbstractClassBlueprint[ScriptContext](bpIn) {

    override val compilerType: CompilerType   = CommonCompilerType.Scalac
    override val rootScope   : RootValueScope = new RootValueScope {
        bindValue("ScriptCode" ~> (_.scriptSourceCode))
        bindValue("ScriptArguments" ~> (_.scriptArguments.map(p => s"${p._1}: ${p._2.getName}").mkString(", ")))
        bindValue("ScriptClass" ~> (_.scriptSuperClass.getName))
    }
}
