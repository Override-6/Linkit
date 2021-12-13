package fr.linkit.examples.ssc.server

import fr.linkit.examples.ssc.api.{CurrentUserAccount, UserAccount, UserWallet}

class UserAccountImpl(current: CurrentUserAccount) extends UserAccount {

    override def getWallets: Seq[UserWallet] = current.getWallets

    override def findWallet(name: String): Option[UserWallet] = current.findWallet(name)
}
