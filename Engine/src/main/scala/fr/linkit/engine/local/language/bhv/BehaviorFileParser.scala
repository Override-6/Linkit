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

package fr.linkit.engine.local.language.bhv

import fr.linkit.engine.local.language.bhv.descriptor.clazz.ClassDescriptor

import java.util.Scanner

class BehaviorFileParser(scanner: Scanner) {

    def makeParse(): Unit = {
        val word = scanner.next()
        word match {
            case "describe" => parseDescriptor()
            case _ => throw new CorruptedBehaviorFileException(s"unknown word : $word at start of file.")
        }
    }

    private def parseDescriptor(): Unit = {
        val descriptor = scanner.next() match {
            case "class" =>
                //describe class {class.name}
                val className = scanner.next()
                val clazz = Class.forName(className)
                new ClassDescriptor(clazz).describe(scanner)
        }

    }

}
