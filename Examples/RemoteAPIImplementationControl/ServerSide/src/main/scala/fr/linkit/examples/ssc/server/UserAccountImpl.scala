package fr.linkit.examples.ssc.server

import fr.linkit.examples.ssc.api.{CurrentUserAccount, UserAccount, UserWallet}

class UserAccountImpl(val current: CurrentUserAccount) extends UserAccount {

    override val getName: String = current.getName

    override def getWallets: Seq[UserWallet] = current.getWallets

    override def findWallet(name: String): UserWallet = current.findWallet(name)
}