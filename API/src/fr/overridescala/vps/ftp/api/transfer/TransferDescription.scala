package fr.overridescala.vps.ftp.api.transfer

import java.net.InetSocketAddress

import fr.overridescala.vps.ftp.api.utils.Constants


class TransferDescription private(val source: FileDescription,
                                  val destination: String,
                                  val targetID: String,
                                  val senderID: String) extends Serializable {

    val transferSize: Long = source size

    override def toString: String = s"TransferDescription{source: ${source.path}, length: $transferSize," +
            s" destination: $destination, target: $targetID, sender: $senderID}"

}

object TransferDescription {

    def builder(): Builder = new Builder()

    class Builder() {
        private var source: FileDescription = _
        private var destination: String = _
        private var targetID: String = _

        def setSource(source: FileDescription): Builder = {
            this.source = source
            this
        }

        def setDestination(destination: String): Builder = {
            this.destination = destination
            this
        }

        def setTargetID(targetID: String): Builder = {
            this.targetID = targetID
            this
        }

        def build(): TransferDescription = new TransferDescription(source, destination, targetID, Constants.SELF_ID)
    }

}

