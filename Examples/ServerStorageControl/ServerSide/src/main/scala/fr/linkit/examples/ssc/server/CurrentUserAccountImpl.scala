package fr.linkit.examples.ssc.server

import fr.linkit.examples.ssc.api.{CurrentUserAccount, CurrentUserWallet, UserWallet}

import scala.collection.mutable

class CurrentUserAccountImpl extends CurrentUserAccount {

    private val wallets = mutable.HashMap.empty[String, CurrentUserWallet]

    override def openWallet(name: String, initialAmount: Int): CurrentUserWallet = {
        if (wallets.contains(name))
            throw new IllegalArgumentException(s"wallet '$name' already exists.")

    }

    override def findWallet(name: String): Option[CurrentUserWallet] = ???

    override def getWallets: Seq[UserWallet] = ???
}
