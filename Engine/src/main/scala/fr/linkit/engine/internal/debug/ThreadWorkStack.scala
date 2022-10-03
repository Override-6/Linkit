package fr.linkit.engine.internal.debug

import java.io.PrintStream
import scala.collection.mutable

class ThreadWorkStack(val thread: Thread) {
    private val actionStack = mutable.Stack.empty[Action]

    def push(actionItem: Action): Unit = {
        actionStack.push(actionItem)
    }

    def isEmpty: Boolean = actionStack.isEmpty

    def pop(): Action = actionStack.pop()

    def printStack(out: PrintStream): Unit = {
        out.print(s"\t- thread ${thread.getName}:")
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
