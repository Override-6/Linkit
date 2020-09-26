package fr.overridescala.vps.ftp.client.cli

import java.util.Scanner

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class CommandManager(input: Scanner) {

    val commands: mutable.Map[String, CommandExecutor] = mutable.Map.empty

    def register(command: String, executor: CommandExecutor): Unit =
        commands.put(command, executor)


    def start(): Unit = {
        while (true) {
            val line = input.nextLine()
            val args = parseLine(line.strip())
            val cmd = line.takeWhile(c => !Character.isWhitespace(c))
            if (commands.contains(cmd)){
                commands(cmd).execute(args)
                    return
            }
            Console.err.println(s"cmd '$cmd' not found.")
        }
    }

    def parseLine(line: String): Array[String] = {
        val argBuilder = new StringBuilder
        val args = ListBuffer.empty[String]

        //exclude first arg, which is the command label
        val indexOfFirstBlankLine = line.indexWhere(Character.isWhitespace)
        if (indexOfFirstBlankLine == -1)
            return Array()
        var insideString = false
        var last = '\u0000'
        for (c <- line) {
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
        args += argBuilder.toString
        args.toArray
    }

}
