package fr.linkit.engine.internal.debug

import java.io.PrintStream
import scala.collection.mutable

class ThreadWorkStack(val thread: Thread) {
    private val actionStack = mutable.Stack.empty[Action]

    def push(actionItem: Action): Unit = {
        actionStack.push(actionItem)
    }

    def pop(): Action = actionStack.pop()

    def printStack(out: PrintStream): Unit = {
        out.print(s"\t- thread ${thread.getName}:")
        if (actionStack.isEmpty) out.println(" <not performing any actions>")
        else out.println("")
        //print from oldest to newest
        actionStack.reverse.foreach(action => out.println("\t\t- " + action.actionType + ": " + action.insights))
    }

}
