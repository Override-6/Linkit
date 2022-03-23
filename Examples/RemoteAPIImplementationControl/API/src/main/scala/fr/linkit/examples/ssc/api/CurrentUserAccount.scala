package fr.linkit.examples.ssc.api

trait CurrentUserAccount extends UserAccount {

    def openWallet(name: String, initialAmount: Int): CurrentUserWallet

    //FIXME method with same name parameter and same return is not supported by scbp class generation
    def findCurrentWallet(name: String): CurrentUserWallet

}
