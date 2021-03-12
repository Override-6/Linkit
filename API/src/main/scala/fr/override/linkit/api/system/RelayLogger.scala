package fr.`override`.linkit.api.system

import org.apache.log4j.{Level, Logger, Priority}

class RelayLogger(name: String) extends Logger(name) {

    override def forcedLog(fqcn: String, level: Priority, message: Any, t: Throwable): Unit = {
        val prefix: String = "" //TODO getPrefix(level)
        super.forcedLog(fqcn, level, prefix + message, t)
    }

}

object RelayLogger {

    import Level._

    private val DefaultPrefix = ""

    private val Prefixes: Map[Priority, String] = Map(
        DEBUG -> "(DEBUG)",
        TRACE -> Console.BLUE,
        WARN -> Console.YELLOW,
        ERROR -> Console.RED,
        FATAL -> Console.RED_B
    )

    def getPrefix(level: Priority): String = {
        Prefixes.getOrElse(level, DefaultPrefix)
    }

}

