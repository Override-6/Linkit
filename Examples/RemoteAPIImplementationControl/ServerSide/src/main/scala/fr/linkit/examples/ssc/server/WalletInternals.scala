package fr.linkit.examples.ssc.server

import fr.linkit.examples.ssc.api.WalletTransaction

trait WalletInternals {

    def finalizeTransaction(transaction: WalletTransaction): Unit

}