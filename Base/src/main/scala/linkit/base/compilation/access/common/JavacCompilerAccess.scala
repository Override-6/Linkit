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

import java.io.File
import java.nio.file.Path
import javax.tools.ToolProvider

object JavacCompilerAccess extends AbstractCompilerAccess {

    val JavaFileExtension = ".java"

    override def getType: CompilerType = CommonCompilerType.Javac

    override def canCompileFile(filePath: Path): Boolean = filePath.toString.endsWith(JavaFileExtension)

    override def compile(sourceFiles: Seq[Path], destination: Path, classPaths: Seq[Path], additionalArguments: Seq[String]): Seq[Path] = {
        val javac                = ToolProvider.getSystemJavaCompiler
        val cpStrings            = classPaths.mkString(File.pathSeparator)
        val options: Seq[String] =
            Seq[String]("-d", destination.toString, "-Xlint:none", "-classpath", cpStrings) ++
                    sourceFiles.map(_.toString) ++
                    additionalArguments
        javac.run(null, null, null, options: _*)
        sourceFiles
                .map(destination.relativize)
                .map(destination.resolve)
    }
}
