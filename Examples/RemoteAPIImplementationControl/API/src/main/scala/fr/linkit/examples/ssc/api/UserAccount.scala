package fr.linkit.examples.ssc.api

trait UserAccount {

    def getName: String

    def getWallets: Seq[UserWallet]

    def findWallet(name: String): UserWallet

}
