package fr.overridescala.vps.ftp.`extension`.fundamental.transfer

class TransferDescriptionBuilder {

    var source: String = null
    var destination: String = null
    var targetID: String = null

    implicit def build(): TransferDescription =
        TransferDescription(source, destination, targetID)

}

object TransferDescriptionBuilder {
    implicit def autoBuild(builder: TransferDescriptionBuilder): TransferDescription =
        builder.build()
}
