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

package fr.linkit.engine.internal.generation.compilation.access.common

import fr.linkit.api.internal.generation.compilation.access.CompilerType
import fr.linkit.engine.internal.generation.compilation.access.{AbstractCompilerAccess, CommonCompilerTypes}

import java.nio.file.Path
import scala.tools.nsc.{Global, Settings}

object ScalacCompilerAccess extends AbstractCompilerAccess {

    val ScalacArguments: List[String] = List("-usejavacp", "-Xplugin:$classes")
    val ScalaFileExtension            = ".scala"

    override def compile(sourceFiles: Seq[Path], destination: Path, classPaths: Seq[Path], additionalArguments: Seq[String]): Seq[Path] = {
        val settings = new Settings()
        settings.processArguments(ScalacArguments ++ Seq("-d", destination.toString) ++ additionalArguments, true)
        val global = new Global(settings)
        val run    = new global.Run
        run.compile(sourceFiles.map(_.toString).toList)
        run.compiledFiles
                .map(str => Path.of(destination + "/" + str.drop(destination.toString.length)))
                .toSeq
    }

    override def getType: CompilerType = CommonCompilerTypes.Scalac

    override def canCompileFile(file: Path): Boolean = {
        file.toString.endsWith(ScalaFileExtension)
    }
}
