package fr.linkit.examples.ssc.api

trait UserAccount {

    def name: String

    def getWallets: Seq[UserWallet]

    def findWallet(name: String): UserWallet

}
