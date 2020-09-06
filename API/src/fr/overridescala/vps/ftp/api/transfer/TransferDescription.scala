package fr.overridescala.vps.ftp.api.transfer

import java.net.InetSocketAddress

import fr.overridescala.vps.ftp.api.utils.Constants


class TransferDescription private(val source: TransferableFile,
                                  val destination: String,
                                  val target: InetSocketAddress,
                                  val sender: InetSocketAddress) extends Serializable {

    val transferSize: Long = source getSize

    override def toString: String = s"TransferDescription{source: ${source.getPath}, length: $transferSize," +
            s" destination: $destination, target: $target, sender: $sender}"

}

object TransferDescription {

    def builder(): Builder = new Builder()

    class Builder() {
        private var source: TransferableFile = _
        private var destination: String = _
        private var target: InetSocketAddress = _

        def setSource(source: TransferableFile): Builder = {
            this.source = source
            this
        }

        def setDestination(destination: String): Builder = {
            this.destination = destination
            this
        }

        def setTarget(target: InetSocketAddress): Builder = {
            this.target = target
            this
        }

        def build(): TransferDescription = new TransferDescription(source, destination, Constants.PUBLIC_ADDRESS, target)
    }

}

