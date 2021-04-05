package fr.linkit.api.connection.packet.serialization.strategy

trait SerialStrategy[A] {

    def getTypeHandling: Class[A]

    def serial(instance: A, serializer: StrategicSerializer): Array[Byte]

    def deserial(bytes: Array[Byte], serializer: StrategicSerializer): Any

}
