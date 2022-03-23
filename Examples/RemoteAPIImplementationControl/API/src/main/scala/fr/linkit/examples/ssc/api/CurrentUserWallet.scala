package fr.linkit.examples.ssc.api

trait CurrentUserWallet extends UserWallet {

    val ownerCurrent: CurrentUserAccount

    def getAmount: Int

    def getHistory: Seq[WalletTransaction]
    
}
