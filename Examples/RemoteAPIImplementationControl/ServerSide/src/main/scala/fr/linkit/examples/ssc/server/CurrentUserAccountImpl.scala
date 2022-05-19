package fr.linkit.examples.ssc.server

import fr.linkit.examples.ssc.api.{CurrentUserAccount, CurrentUserWallet, UserWallet}

import scala.collection.mutable

class CurrentUserAccountImpl(override val getName: String) extends CurrentUserAccount {

    private val wallets = mutable.HashMap.empty[String, CurrentUserWallet]

    override def openWallet(name: String, initialAmount: Int): CurrentUserWallet = {
        if (wallets.contains(name))
            throw new IllegalArgumentException(s"wallet '$name' already exists.")
        wallets.getOrElseUpdate(name, new CurrentUserWalletImpl(this, name, initialAmount))
    }

    override def findWallet(name: String): UserWallet = wallets.get(name).orNull

    override def findCurrentWallet(name: String): CurrentUserWallet = wallets.get(name).orNull

    override def getWallets: Seq[UserWallet] = wallets.values.toSeq
}