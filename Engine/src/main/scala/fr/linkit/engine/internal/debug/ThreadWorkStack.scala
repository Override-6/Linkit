package fr.linkit.engine.internal.debug

import java.io.PrintStream
import scala.collection.mutable

class ThreadWorkStack(val threadName: String) {
    private val actionStack = mutable.Stack.empty[State]

    def push(actionItem: State): Unit = {
        actionStack.push(actionItem)
    }

    def isEmpty: Boolean = actionStack.isEmpty

    def pop(): State = actionStack.pop()

    def printStack(out: PrintStream): Unit = {
        out.print(s"\t- thread $threadName:")
        if (actionStack.isEmpty) {
            out.println(" <not performing any actions>")
            return
        } else out.println("")
        //print from oldest to newest
        val maxActionTypeStrLength = actionStack.map(_.actionType.length).max
        val maxTaskPathStrLength   = actionStack.map(_.taskPath.mkString(">").length).max
        actionStack.reverse.foreach(action => {
            val taskPath   = action.taskPath.mkString(">")
            val actionType = action.actionType
            out.println("\t\t- " + taskPath + (" " * (maxTaskPathStrLength - taskPath.length - 1)) +
                            " " + actionType +
                            (" " * (maxActionTypeStrLength - actionType.length)) +
                            ": " + action.insights)
        })
    }

}
