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

package linkit.base.compilation.access.common

import linkit.base.compilation.access.{AbstractCompilerAccess, CommonCompilerType}

import java.nio.file.{Files, Path}
import scala.tools.nsc.{Global, Settings}

object ScalacCompilerAccess extends AbstractCompilerAccess {

    val ScalacDefaultArguments: List[String] = List("-usejavacp", "-nowarn")
    val ScalaFileExtension                   = ".scala"

    override def compile(sourceFiles: Seq[Path], destination: Path, classPaths: Seq[Path], additionalArguments: Seq[String]): Seq[Path] = {
        if (Files.notExists(destination))
            Files.createDirectories(destination)
        val settings = new Settings()
        val arguments = ScalacDefaultArguments ++ Seq("-d", destination.toString) ++ additionalArguments
        settings.processArguments(arguments, true)
        classPaths.map(_.toString).foreach(settings.classpath.append)
        val global = new Global(settings)
        val run    = new global.Run
        run.compile(sourceFiles.map(_.toString).toList)
        run.compiledFiles
                .map(str => Path.of(destination + "/" + str.drop(destination.toString.length)))
                .toSeq
    }

    override def getType: CompilerType = CommonCompilerType.Scalac

    override def canCompileFile(file: Path): Boolean = {
        file.toString.endsWith(ScalaFileExtension)
    }
}
