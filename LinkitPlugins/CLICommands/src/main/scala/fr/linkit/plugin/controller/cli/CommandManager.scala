package fr.linkit.plugin.controller.cli

import fr.linkit.core.local.plugin.fragment.LinkitPluginFragment

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class CommandManager extends LinkitPluginFragment {

    private val commands: mutable.Map[String, CommandExecutor] = mutable.Map.empty
    @volatile private var alive = true

    def register(command: String, executor: CommandExecutor): Unit =
        commands.put(command.toLowerCase, executor)

    override def start(): Unit = {
        val thread = new Thread(() => {
            while (alive)
                perform(InputConsole.requestNextInput())
        })

        thread.setName("Command listener Thread")
        thread.start()
    }

    def perform(command: String): Unit = {
        if (command == null)
            return
        val args = parseLine(command.trim())
        val cmd = command.takeWhile(c => !Character.isWhitespace(c)).toLowerCase
        if (!commands.contains(cmd)) {
            Console.err.println(s"cmd '$cmd' not found.")
            return
        }

        try {
            commands(cmd).execute(args)
        } catch {
            case e@(_: CommandException) => Console.err.println(e.getMessage)
            case NonFatal(e)                               => e.printStackTrace()
        }
    }

    private def parseLine(line: String): Array[String] = {
        val argBuilder = new StringBuilder
        val args = ListBuffer.empty[String]

        //exclude first arg, which is the command label
        val indexOfFirstBlankLine = line.indexWhere(Character.isWhitespace)
        if (indexOfFirstBlankLine == -1)
            return Array()
        val rawArgs = line.substring(indexOfFirstBlankLine).trim()

        var insideString = false
        var last = '\u0000'
        for (c <- rawArgs) {
            if (c == '"' && last != '\\')
                insideString = !insideString
            else if (!c.isWhitespace || (insideString && last != '\\'))
                argBuilder.append(c)
            else if (!last.isWhitespace) {
                args += argBuilder.toString()
                argBuilder.clear()
            }
            last = c
        }
        args += argBuilder.toString()
        args.toArray
    }

    override def destroy(): Unit = {
        commands.clear()
        alive = false
    }
}
