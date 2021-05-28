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

package fr.linkit.engine.local.generation.access

import fr.linkit.api.local.generation.{CompilerAccess, CompilerCenter}
import fr.linkit.engine.local.generation.access.common.{JavacCompilerAccess, ScalacCompilerAccess}

import java.nio.file.Path
import scala.collection.mutable.ListBuffer

class DefaultCompilerCenter extends CompilerCenter {

    private val compilers = ListBuffer.from(Seq(JavacCompilerAccess, ScalacCompilerAccess))

    override def addAccess(access: CompilerAccess): Unit = compilers += access

    override def getAccessForFile(filePath: Path): Option[CompilerAccess] = compilers.find(_.canCompileFile(filePath))

    override def compileAll(files: Array[Path], destination: Path, classPaths: Seq[Path]): Unit = {
        files.map(f => (getAccessForFile(f), f))
                .filterNot(_._1.isEmpty)
                .groupBy(_._1.get)
                .map(t => (t._1, t._2.map(_._2)))
                .foreach(t => {
                    val access = t._1
                    val paths  = t._2
                    access.compileAll(paths, destination, classPaths)
                })
    }

    override def compileAll(folder: Path, destination: Path, classPaths: Seq[Path]): Unit = {
        compilers.foreach(_.compileAll(folder, destination, classPaths))
    }
}
