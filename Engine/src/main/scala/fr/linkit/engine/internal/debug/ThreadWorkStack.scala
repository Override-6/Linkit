package fr.linkit.engine.internal.debug

import java.io.PrintStream
import scala.collection.mutable

class ThreadWorkStack(val threadName: String) {
    private val stepStack = mutable.Stack.empty[Step]

    def push(step: Step): Unit = {
        stepStack.push(step)
    }

    def isEmpty: Boolean = stepStack.isEmpty

    def pop(): Step = stepStack.pop()

    def printStack(out: PrintStream): Unit = {
        out.print(s"\t- thread $threadName:")
        if (stepStack.isEmpty) {
            out.println(" <not performing any work>")
            return
        } else out.println("")
        //print from oldest to newest
        val maxActionTypeStrLength = stepStack.map(_.actionType.length).max
        val maxTaskPathStrLength   = stepStack.map(_.taskPath.mkString(">").length).max
        stepStack.reverse.foreach(action => {
            val taskPath   = action.taskPath.mkString(">")
            val actionType = action.actionType
            out.println("\t\t- " + taskPath + (" " * (maxTaskPathStrLength - taskPath.length - 1)) +
                            " " + actionType +
                            (" " * (maxActionTypeStrLength - actionType.length)) +
                            ": " + action.insights)
        })
    }

}
