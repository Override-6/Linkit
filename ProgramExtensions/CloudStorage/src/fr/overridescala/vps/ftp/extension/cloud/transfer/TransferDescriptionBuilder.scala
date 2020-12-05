package fr.overridescala.vps.ftp.`extension`.cloud.transfer

class TransferDescriptionBuilder {

    var source: String = _
    var destination: String = _
    var targetID: String = _

    implicit def build(): TransferDescription =
        TransferDescription(source, destination, targetID)

}

object TransferDescriptionBuilder {
    implicit def autoBuild(builder: TransferDescriptionBuilder): TransferDescription =
        builder.build()
}
