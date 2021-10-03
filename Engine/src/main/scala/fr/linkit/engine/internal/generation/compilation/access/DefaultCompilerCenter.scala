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

package fr.linkit.engine.internal.generation.compilation.access

import fr.linkit.api.internal.generation.compilation.access.CompilerAccess
import fr.linkit.api.internal.generation.compilation.{CompilationRequest, CompilationResult, CompilerCenter}
import fr.linkit.engine.internal.generation.compilation.access.common.{JavacCompilerAccess, ScalacCompilerAccess}

import java.nio.file.{Files, Path}
import scala.collection.mutable

class DefaultCompilerCenter extends CompilerCenter {

    private val defaultAccess = ScalacCompilerAccess
    private val accesses      = mutable.HashSet.from[CompilerAccess](Seq(defaultAccess, JavacCompilerAccess))

    override def addAccess(access: CompilerAccess): Unit = accesses += access

    override def getAccessForFile(filePath: Path): Option[CompilerAccess] = accesses.find(_.canCompileFile(filePath))

    override def compileAll(files: Seq[Path], destination: Path, classPaths: Seq[Path]): Unit = {
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

    override def compileAll(folder: Path, recursively: Boolean, destination: Path, classPaths: Seq[Path]): Unit = {
        val files = {
            def collect(f: Path): Seq[Path] = {
                if (Files.isDirectory(f)) Files.list(f).toArray((i: Int) => new Array[Path](i)).flatMap(collect)
                else Seq(f)
            }

            if (recursively) collect(folder) else
                Files.list(folder)
                        .toArray(new Array[Path](_))
                        .toSeq
        }
        compileAll(files, destination, classPaths)
    }

    override def processRequest[A](request: CompilationRequest[A]): CompilationResult[A] = {
        val restAccesses = accesses.clone()
        var results = request
                .compilationOrder
                .flatMap(kind => {
                    accesses.filter(_.getType == kind)
                            .tapEach(restAccesses.remove)
                            .map(_.compileRequest(request))
                })
        results ++= restAccesses.map(_.compileRequest(request))
        val outs = results.flatMap(_.getOuterFiles)
        val compileTime = results.map(_.getCompileTime).sum
        request.conclude(outs, compileTime)
    }

}
