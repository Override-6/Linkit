package fr.linkit.examples.ssc.server


import java.sql.Timestamp
import java.util.Random

import fr.linkit.examples.ssc.api.{CurrentUserWallet, UserWallet, WalletTransaction}

import scala.collection.mutable.ListBuffer

class CurrentUserWalletImpl(override val name: String) extends CurrentUserWallet {

    private var amount  = 0
    private val history = ListBuffer.empty[WalletTransaction]

    override def getAmount: Int = amount

    override def getHistory: Seq[WalletTransaction] = history.toSeq

    override def sendMoney(amount: Int, to: UserWallet): Unit = {
        if (this.amount < amount)
            throw new IllegalStateException("not enough money to perform transaction.")
        this.amount -= amount
        to.asInstanceOf[WalletInternals].addAmount(amount)
        
        val now = new Timestamp(System.currentTimeMillis())
        history += WalletTransaction(getTransactionID, amount, this, to, now)
    }

    private def getTransactionID: Int = new Random().nextInt()
}
