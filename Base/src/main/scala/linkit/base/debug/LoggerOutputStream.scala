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

package linkit.base.debug

import org.apache.logging.log4j.{Level, Logger}

import java.io.OutputStream
import scala.collection.mutable

class LoggerOutputStream(logger: Logger, level: Level) extends OutputStream() {

    private var buffer = new mutable.StringBuilder()

    override def write(b: Array[Byte], off: Int, len: Int): Unit = {
        buffer.append(new String(b, off, len))
        flush()
    }

    override def write(b: Int): Unit = {
        buffer.append(b.toChar)
        flush()
    }

    override def flush(): Unit = {
        var lineIndex = buffer.indexOf('\n')
        while (lineIndex != -1) {
            val line = buffer.substring(0, lineIndex)
            logger.log(level, line)
            buffer = buffer.drop(lineIndex + 1)
            lineIndex = buffer.indexOf('\n')
        }
    }
}
