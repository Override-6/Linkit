package fr.overridescala.vps.ftp.client.cli

object CommandUtils {

    def checkArgsContains(args: Array[String], expected: String*): Unit = {
        val success = expected.forall(args.contains)
        if (success)
            return

        val errorMsg = s"missing or wrong argument in command syntax. Expected : $expected"
        throw new IllegalArgumentException(errorMsg)
    }


    def argAfter(args: Array[String], ref: String): String =
        args(args.indexOf(ref) + 1)

}
