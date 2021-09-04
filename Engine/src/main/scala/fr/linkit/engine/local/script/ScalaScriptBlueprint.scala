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

import fr.linkit.api.local.generation.compilation.access.CompilerType
import fr.linkit.engine.connection.packet.persistence.context.script.ScriptConfigContext
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.generation.compilation.access.CommonCompilerTypes
import fr.linkit.engine.local.language.cbp.AbstractClassBlueprint

import java.io.InputStream

class ScalaScriptBlueprint(bpIn: InputStream) extends AbstractClassBlueprint[ScriptConfigContext](bpIn) {

    override val compilerType: CompilerType   = CommonCompilerTypes.Scalac
    override val rootScope   : RootValueScope = new RootValueScope {
        bindValue("ScriptCode" ~> (_.scriptSourceCode))
    }
}
