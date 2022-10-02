package fr.linkit.engine.internal.debug

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
