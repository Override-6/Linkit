package fr.linkit.examples.ssc.api

trait UserWallet {

    def owner: UserAccount

    def name: String

    def sendMoney(amount: Int, to: UserWallet): Unit

}