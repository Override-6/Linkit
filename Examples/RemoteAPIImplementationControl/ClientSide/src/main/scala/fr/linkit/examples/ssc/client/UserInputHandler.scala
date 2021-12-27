package fr.linkit.examples.ssc.client

import fr.linkit.examples.ssc.api.UserAccountContainer

import scala.util.control.NonFatal

class UserInputHandler(accounts: UserAccountContainer) {

    def performCommand(cmd: String, args: Array[String]): Unit = try {
        cmd match {
            case "open"       => openWallet(args)
            case "send"       => sendMoney(args)
            case "getHistory" => printHistory(args)
            case _            => Console.err.println("usage: open|send|getHistory")
        }
    } catch {
        case e: CmdException => Console.err.println(e.getMessage)
        case NonFatal(e)     => e.printStackTrace()
    }

    private def openWallet(args: Array[String]): Unit = {
        if (args.length != 2)
            fail("usage: open <wallet_name> <initial_amount>")
        val current       = accounts.getCurrentAccount
        val walletName    = args(0)
        val initialAmount = args(1).toInt
        Option(current.findWallet(walletName)).fold(current.openWallet(walletName, initialAmount)) { _ =>
            fail(s"Wallet '$walletName' already exists !")
        }
        println(s"Successfully opened walled '$walletName' with initial amount of $initialAmount$$")
    }

    private def printHistory(args: Array[String]): Unit = {
        if (args.length != 1)
            fail("usage: getHistory <wallet_name>")
        val walletName = args.head
        Option(accounts.getCurrentAccount
                .findCurrentWallet(walletName))
                .getOrElse(fail(s"could not find wallet '$walletName'"))
                .getHistory
                .foreach(println)
    }

    private def sendMoney(args: Array[String]): Unit = {
        if (args.length != 4) {
            fail("usage: send <amount> <current_wallet_name> <target_user_name>, <target_user_wallet_name>")
        }
        val amount               = args(0).toInt
        val currentWalletName    = args(1)
        val targetUserName       = args(2)
        val targetUserWalletName = args(3)

        val current      = accounts.getCurrentAccount
        val wallet       = Option(current.findCurrentWallet(currentWalletName)).getOrElse(fail(s"could not find wallet '$currentWalletName'"))
        val target       = accounts.getAccount(targetUserName)
        val targetWallet = Option(target.findWallet(targetUserWalletName)).getOrElse(fail(s"could not find wallet '$targetUserWalletName' into '$targetUserName' account."))
        wallet.sendMoney(amount, targetWallet)
        println("Money sent !")
        println(s"Current amount for wallet $currentWalletName : ${wallet.getAmount}")
    }

    private class CmdException(msg: String) extends Exception(msg)

    private def fail(msg: String): Nothing = {
        throw new CmdException(msg)
    }

}
