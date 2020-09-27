package fr.overridescala.vps.ftp.client.cli

import java.util.Scanner

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class CommandManager(input: Scanner) {

    val commands: mutable.Map[String, CommandExecutor] = mutable.Map.empty

    def register(command: String, executor: CommandExecutor): Unit =
        commands.put(command.toLowerCase, executor)


    def start(): Unit = {
        while (true)
            handleNextInput()
    }

    def handleNextInput(): Unit = {
        val line = input.nextLine()
        val args = parseLine(line.strip())
        val cmd = line.takeWhile(c => !Character.isWhitespace(c)).toLowerCase
        if (commands.contains(cmd)) {
            try {
                commands(cmd).execute(args)
            } catch {
                case e: Throwable => e.printStackTrace()
            }
            return
        }
        Console.err.println(s"cmd '$cmd' not found.")
    }

    def parseLine(line: String): Array[String] = {
        val argBuilder = new StringBuilder
        val args = ListBuffer.empty[String]

        //exclude first arg, which is the command label
        val indexOfFirstBlankLine = line.indexWhere(Character.isWhitespace)
        if (indexOfFirstBlankLine == -1)
            return Array()
        val rawArgs = line.substring(indexOfFirstBlankLine).strip()

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

}
