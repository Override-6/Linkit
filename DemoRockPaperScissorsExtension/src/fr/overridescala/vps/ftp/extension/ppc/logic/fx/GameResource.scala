package fr.overridescala.vps.ftp.`extension`.ppc.logic.fx

import javafx.scene.image.Image


object GameResource {


    /**
     * Texture constants
     * */
    val NothingIcon: Image = img("/rien.png")
    val RockIcon: Image = img("/pierre.png")
    val ScissorsIcon: Image = img("/ciseaux.png")
    val PaperIcon: Image = img("/papier.png")
    val VersusIcon: Image = img("/versus.png")
    val Validate: Image = img("/validate.png")
    val ArrowOff: Image = img("/arrow_off.png")
    val ArrowOn: Image = img("/arrow_on.png")
    val UnknownIcon: Image = img("/unknown.png")

    private def img(name: String): Image = new Image(getClass.getResourceAsStream(name))

}
