package fr.linkit.examples.ssc.api.transaction

import fr.linkit.examples.ssc.api.UserWallet

trait WalletTransactionControl {

    def makeTransaction(amount: Int, from: UserWallet, to: UserWallet): WalletTransaction

}
