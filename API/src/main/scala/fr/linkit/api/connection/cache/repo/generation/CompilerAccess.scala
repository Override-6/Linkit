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

package fr.linkit.api.connection.cache.repo.generation

import java.nio.file.Path

trait CompilerAccess {

    /**
     * This methods will compile all classes that can be compiled with the
     * implemented compiler from the "sourceFolder" path. All compiled classes
     * will then be put in the destination folder.
     *
     * @param sourceFolder absolute path to the folder in which sources are put.
     * @param destination absolute path to the folder in which the compiler will put the generated classes.
     * @return the compiler's exit code.
     * @throws InvalidPuppetDefException if the compilation did not completed successfully
     * */
    def compileAll(sourceFolder: Path, destination: Path, classPaths: Seq[Path]): Int

    /**
     * This methods will compile all classes that can be compiled with the
     * implemented compiler from the "sourceFolder" path. All compiled classes
     * will then be put in the destination folder.
     *
     * @param sourceFiles absolute path to each file the implemented compiler must compute.
     * @param destination absolute path to the folder in which the compiler will put the generated classes.
     * @return the compiler's exit code.
     * @throws InvalidPuppetDefException if the compilation did not completed successfully
     * */
    def compileAll(sourceFiles: Array[Path], destination: Path, classPaths: Seq[Path]): Int

}
