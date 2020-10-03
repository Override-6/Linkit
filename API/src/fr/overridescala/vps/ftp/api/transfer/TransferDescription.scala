package fr.overridescala.vps.ftp.api.transfer

/**
 * Description of a Download / Upload transfer.
 *
 * @param source the FileDescription to download or upload
 * @param destination where the file / folder will be stored
 * @param targetID the identifier of the Relay to perform the transfer
 * */
case class TransferDescription(source: FileDescription,
                               destination: String,
                               targetID: String) extends Serializable {

    val transferSize: Long = source.size

    /**
     * reverse the description.
     * @param targetID the identifier to flip
     * @return a cloned [[TransferDescription]] with targetID respecified
     * */
    def reversed(targetID: String): TransferDescription =
        TransferDescription(source, destination, targetID)

}


