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

package fr.linkit.api.internal.system

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.internal.concurrency.WorkerPools
import org.apache.log4j.{Level, Logger}
import org.jetbrains.annotations.Nullable

import scala.collection.mutable.ListBuffer

object AppLogger {
    
    var networkPreviewLength: Int = 1000
    
    var useVerbose         : Boolean = false
    
    var printTraffic  : Boolean = true
    var printTaskStack: Boolean = true
    
    val logger: Logger = Logger.getLogger(classOf[ApplicationContext])
    
    def trace(msg: AnyRef): Unit = logger.trace(applyMsg(msg))
    
    def vTrace(msg: => AnyRef): Unit = verbose {
        trace(msg)
    }
    
    def trace(msg: AnyRef, throwable: Throwable): Unit = {
        logger.trace(applyMsg(msg), throwable)
    }
    
    def info(msg: AnyRef): Unit = logger.info(applyMsg(msg))
    
    def vInfo(msg: => AnyRef): Unit = verbose {
        info(msg)
    }
    
    def info(msg: AnyRef, throwable: Throwable): Unit = logger.info(applyMsg(msg), throwable)
    
    def warn(msg: AnyRef): Unit = logger.warn(applyMsg(msg))
    
    def vWarn(msg: => AnyRef): Unit = verbose {
        warn(msg)
    }
    
    def warn(msg: AnyRef, throwable: Throwable): Unit = logger.warn(applyMsg(msg), throwable)
    
    def error(msg: AnyRef): Unit = logger.error(applyMsg(msg))
    
    def vError(msg: => AnyRef): Unit = verbose {
        error(msg)
    }
    
    def error(msg: AnyRef, throwable: Throwable): Unit = logger.error(applyMsg(msg), throwable)
    
    def fatal(msg: AnyRef): Unit = logger.fatal(applyMsg(msg))
    
    def vFatal(msg: => AnyRef): Unit = verbose {
        fatal(msg)
    }
    
    def debug(msg: AnyRef): Unit = logger.debug(applyMsg(msg))
    
    def vDebug(msg: => AnyRef): Unit = verbose {
        debug(msg)
    }
    
    def logUpload(target: String, bytes: Array[Byte]): Unit = /*verbose*/ {
        if (printTraffic) {
            val preview = new String(bytes.take(networkPreviewLength)).replace('\n', ' ').replace('\r', ' ')
            logger.debug(s"${Console.MAGENTA}Written : ↑ $target ↑ $preview (l: ${bytes.length})")
        }
    }
    
    def logDownload(@Nullable target: String, bytes: Array[Byte]): Unit = /*verbose*/ {
        if (printTraffic) {
            val preview     = new String(bytes.take(networkPreviewLength)).replace('\n', ' ').replace('\r', ' ')
            val finalTarget = if (target == null) "" else target
            logger.debug(s"${Console.CYAN}Received: ↓ $finalTarget ↓ $preview (l: ${bytes.length + 4})")
        }
    }
    
    def log(level: Level, msg: AnyRef): Unit = logger.log(level, applyMsg(msg))
    
    def log(level: Level, msg: AnyRef, throwable: Throwable): Unit = logger.log(level, applyMsg(msg), throwable)
    
    def printStackTrace(e: Throwable): Unit = {
        logger.error(s"Exception in thread '${Thread.currentThread().getName}': " + e.getMessage)
        e.printStackTrace()
    }
    
    def discoverLines(from: Int, to: Int): Unit = verbose {
        val currentThread = Thread.currentThread()
        val stackTrace    = currentThread.getStackTrace
        
        for (i <- (from + 2) to to.min(stackTrace.length - 2) + 1) {
            println("\t at: " + stackTrace(i))
        }
    }
    
    private var totalVerbPrints = 0
    private val verboseLines    = ListBuffer.empty[() => Unit]
    
    private def applyMsg(msg: AnyRef): String = {
        if (!printTaskStack) msg.toString
        else {
            (WorkerPools.currentWorkerOpt match {
                case None         => "(?)"
                case Some(worker) => worker.getTaskStack.mkString("(", ">", ")")
            }) + " " +msg.toString
        }
    }
    
    private def verbose(action: => Unit): Unit = {
        if (useVerbose)
            action
    }
    
}
