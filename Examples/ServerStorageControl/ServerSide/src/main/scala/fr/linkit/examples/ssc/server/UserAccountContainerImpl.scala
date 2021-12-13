package fr.linkit.examples.ssc.server

import fr.linkit.examples.ssc.api.{CurrentUserAccount, UserAccount, UserAccountContainer}

import scala.collection.mutable

class UserAccountContainerImpl extends UserAccountContainer {

    private val accounts = mutable.HashMap.empty[String, UserAccountImpl]

    override def getCurrentAccount: CurrentUserAccount = {

    }

    override def getAccount(name: String): UserAccount = ???

    override def listAccounts: Seq[UserAccount] = ???
}
