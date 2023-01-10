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

import fr.linkit.engine.internal.debug.cli.SectionedPrinter

import scala.Console.out
import scala.collection.mutable

class ThreadWorkStack(val thread: Thread) {
    private val stepStack = mutable.Stack.empty[Step]

    def push(step: Step): Unit = {
        stepStack.push(step)
    }

    def isEmpty: Boolean = stepStack.isEmpty

    def pop(): Step = stepStack.pop()

    def printStack(printer: SectionedPrinter): Unit = {
        import printer._
        val section = printer.newSection().enable()
        section.append(s"\t- thread ${thread.getName}:")
        if (stepStack.isEmpty) {
            section.append(" <not performing any work>\n")
            return
        } else section.append("\n")
        //print from oldest to newest
        val maxActionTypeStrLength = stepStack.map(_.actionType.length).max
        val maxTaskPathStrLength   = stepStack.map(_.taskID.toString.length).max
        stepStack.reverse.foreach(action => {
            val taskID     = action.taskID
            val actionType = action.actionType
            section.append("\t\t- " + (if (taskID == -1) "?" else taskID) + (" " * (maxTaskPathStrLength - taskID.toString.length - 1)) +
                               " " + actionType +
                               (" " * (maxActionTypeStrLength - actionType.length)) +
                               ": " + action.insights).append('\n')
        })
        section.flush()
    }

}
