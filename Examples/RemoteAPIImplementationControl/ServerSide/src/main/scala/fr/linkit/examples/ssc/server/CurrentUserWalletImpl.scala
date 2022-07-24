package fr.linkit.examples.ssc.server


import java.sql.Timestamp
import java.util.Random
import fr.linkit.examples.ssc.api.{CurrentUserAccount, CurrentUserWallet, UserAccount, UserWallet, WalletTransaction}

import scala.collection.mutable.ListBuffer

class CurrentUserWalletImpl(override val ownerCurrent: CurrentUserAccount,
                            override val name: String,
                            private var amount: Int) extends CurrentUserWallet with WalletInternals {

    override val owner: UserAccount = ownerCurrent

    private val history = ListBuffer.empty[WalletTransaction]

    override def getAmount: Int = amount


    override def finalizeTransaction(transaction: WalletTransaction): Unit = {
        history += transaction
        this.amount += transaction.amount
    }

    override def getHistory: Seq[WalletTransaction] = history.toSeq

    override def sendMoney(amount: Int, to: UserWallet): Unit = {
        if (this.amount < amount)
            throw new IllegalStateException("not enough money to perform transaction.")
        if (to eq this)
            throw new IllegalArgumentException("can't send money to self")
        this.amount -= amount

        val now = new Timestamp(System.currentTimeMillis())
        val transaction = WalletTransaction(getTransactionID, amount, this, to, now)
        to.asInstanceOf[WalletInternals].finalizeTransaction(transaction)

        history += transaction
    }

    private def getTransactionID: Int = new Random().nextInt()
}
