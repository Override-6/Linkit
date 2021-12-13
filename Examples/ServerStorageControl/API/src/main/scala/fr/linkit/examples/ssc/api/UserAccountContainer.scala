package fr.linkit.examples.ssc.api

trait UserAccountContainer {

    def getCurrentAccount: CurrentUserAccount

    def getAccount(name: String): UserAccount

    def listAccounts: Seq[UserAccount]

}
