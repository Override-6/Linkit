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

package fr.linkit.api.internal.generation.compilation

import fr.linkit.api.internal.generation.compilation.access.CompilerType

import java.io.{InputStream, OutputStream}
import java.nio.file.Path

trait CompilationRequest[T] {

    val classPaths: Seq[Path]

    val workingDirectory: Path

    lazy val sourcesDir: Path = Path.of(workingDirectory + "/Sources/")

    lazy val classDir: Path = Path.of(workingDirectory + "/Classes/")

    val compilationOrder: Seq[CompilerType] = Seq.empty

    def sourceCodesPaths: Seq[Path]

    def compilerInput: InputStream = System.in

    def compilerOutput: OutputStream = System.out

    def compilerErrOutput: OutputStream = System.err

    def additionalParams(cType: CompilerType): Array[String]

    def conclude(outs: Seq[Path], compilationTime: Long): CompilationResult[T]
}
