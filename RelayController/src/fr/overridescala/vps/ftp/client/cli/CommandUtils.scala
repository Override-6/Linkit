package fr.overridescala.vps.ftp.client.cli

object CommandUtils {

    def checkArgsContains(expected: String*)(implicit args: Array[String]): Unit = {
        val success = expected.forall(args.contains)
        if (success)
            return

        val errorMsg = s"missing or wrong argument in command syntax. Expected : ${expected.mkString(" and ")}"
        throw CommandException(errorMsg)
    }


    def argAfter(ref: String)(implicit args: Array[String]): String =
        args(args.indexOf(ref) + 1)

}
