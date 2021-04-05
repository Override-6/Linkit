package fr.linkit.api.connection.packet.serialization.strategy

trait StrategyHolder {

    def drainAllStrategies(holder: StrategyHolder): Unit

    def attachStrategy(strategy: SerialStrategy[_]): Unit

}
