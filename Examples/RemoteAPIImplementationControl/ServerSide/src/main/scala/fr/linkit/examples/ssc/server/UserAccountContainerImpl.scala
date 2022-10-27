package fr.linkit.examples.ssc.server

import fr.linkit.api.gnom.network.ExecutorEngine
import fr.linkit.examples.ssc.api.{CurrentUserAccount, UserAccount, UserAccountContainer}

import scala.collection.mutable

class UserAccountContainerImpl extends UserAccountContainer {

    private val accounts = mutable.HashMap.empty[String, UserAccountImpl]

    override def getCurrentAccount: CurrentUserAccount = {
        val executor   = ExecutorEngine.currentEngine
        val identifier = executor.name
        accounts.getOrElseUpdate(identifier, openAccount(identifier)).current
    }

    override def getAccount(name: String): UserAccount = {
        accounts.getOrElse(name, throw new NoSuchElementException(s"Could not find account '$name'"))
    }

    override def listAccounts: Seq[UserAccount] = accounts.values.toSeq

    private def openAccount(name: String): UserAccountImpl = {
        val currentAccount = new CurrentUserAccountImpl(name)
        new UserAccountImpl(currentAccount)
    }
}
