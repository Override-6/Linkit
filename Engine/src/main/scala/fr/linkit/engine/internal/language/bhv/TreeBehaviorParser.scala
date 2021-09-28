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

package fr.linkit.engine.internal.language.bhv

import java.io.{BufferedReader, InputStream}
import java.nio.file.{Files, Path}
import java.util.Scanner
import java.util.regex.Pattern

object TreeBehaviorParser {

    def parseFile(behaviorFile: Path): Unit = {
        parse(Files.newBufferedReader(behaviorFile))
    }

    def parse(reader: BufferedReader): Unit = {
        parse(new Scanner(reader))
    }

    def parse(in: InputStream): Unit = {
        parse(new Scanner(in))
    }

    def parse(scanner: Scanner): Unit = {
        //scanner.useDelimiter(Pattern.compile("(/\\*.*\\*/)|(//.+\\n)", Pattern.DOTALL))
        new BehaviorFileParser(scanner).makeParse()
    }

}
