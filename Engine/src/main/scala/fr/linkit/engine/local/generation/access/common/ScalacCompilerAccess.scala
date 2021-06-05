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

package fr.linkit.engine.local.generation.access.common

import fr.linkit.api.local.generation.CompilerType
import fr.linkit.engine.local.generation.access.{AbstractCompilerAccess, CommonCompilerTypes}

import java.nio.file.Path
import scala.tools.nsc.{Global, Settings}

object ScalacCompilerAccess extends AbstractCompilerAccess {

    val ScalacArguments: List[String] = List("-usejavacp", "-Xplugin:$classes")
    val ScalaFileExtension            = ".scala"

    override def compile(sourceFiles: Array[Path], destination: Path, classPaths: Seq[Path]): Int = {
        val settings = new Settings()
        settings.processArguments(ScalacArguments ++ List("-d", destination.toString), true)
        val global = new Global(settings)
        val run = new global.Run
        run.compile(sourceFiles.map(_.toString).toList)
        0 //OK code
    }

    override def getType: CompilerType = CommonCompilerTypes.Scalac

    override def canCompileFile(file: Path): Boolean = {
        val v = file.toString.endsWith(ScalaFileExtension)
        v
    }
}
