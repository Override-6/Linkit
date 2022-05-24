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

package fr.linkit.api.internal.system.log

import fr.linkit.api.internal.concurrency.WorkerPools
import fr.linkit.api.internal.system.log.WorkerTaskStackConverter._
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.pattern._

import java.lang
import scala.util.Try

@Plugin(name = "task_stack", category = PatternConverter.CATEGORY)
@ConverterKeys(Array("task_stack", "stack", "ts"))
class WorkerTaskStackConverter(options: Array[String]) extends LogEventPatternConverter("task stack", "default") {
    
    private val separator = Try(options(0)).getOrElse(">")
    private val unknown   = Try(options(1)).getOrElse("?")
    
    override def format(event: LogEvent, sb: lang.StringBuilder): Unit = {
        val start = sb.length()
        if (fullFormat(sb)) {
            fieldColor.format(start, sb)
        } else fieldNoColor.format(start, sb)
    }
    
    private def fullFormat(sb: lang.StringBuilder): Boolean = {
        WorkerPools.currentWorkerOpt match {
            case None         =>
                sb.append(unknown)
                false
            case Some(worker) =>
                val stack = worker.getTaskStack
                if (stack.isEmpty) {
                    sb.append("-")
                    false
                }
                else {
                    var i = 0
                    while (i < stack.length - 1) {
                        sb.append(stack(i)).append(separator)
                        i += 1
                    }
                    val last = stack(i)
                    sb.append(green).append(last)
                    true
                }
        }
    }
    
}

object WorkerTaskStackConverter {
    
    private final val green        = AnsiEscape.createSequence("YELLOW")
    private final val MinLength    = 6
    private final val MaxLength    = 16
    private final val fieldNoColor = new FormattingInfo(true, MinLength, MaxLength, true)
    private final val fieldColor   = new FormattingInfo(true, MinLength + green.length, MaxLength + green.length, true)
    
    def newInstance(options: Array[String]): WorkerTaskStackConverter = {
        new WorkerTaskStackConverter(options.headOption.map(_.split(",")).getOrElse(Array()))
    }
}
