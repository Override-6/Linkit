package fr.overridescala.vps.ftp.extension.ppc.logic;

import fr.overridescala.vps.ftp.extension.ppc.logic.fx.GameResource;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.NoSuchElementException;

/**
 * énumération qui représente le type de coup (pierre, papier ciseaux)
 * contient : sa traduction en Français, et son icone pour sa représentation graphique.
 * */
public enum MoveType {

    ROCK("PIERRE", GameResource.RockIcon()),
    PAPER("PAPIER", GameResource.PaperIcon()),
    SCISSORS("CISEAUX", GameResource.ScissorsIcon());

    private final String translate;
    private final Image icon;

    /**
     * @param translate la traduction en français
     * @param icon représentation graphique
     * */
    MoveType(String translate, Image icon) {
        this.translate = translate;
        this.icon = icon;
    }

    public ImageView getTexture() {
        return new ImageView(icon);
    }

    public Image getIcon() {
        return icon;
    }

    /**
     * retourne le type de coup par apport à sa traduction.
     * */
    public static MoveType valueOfFrenchName(String frenchName) {
        frenchName = frenchName.toUpperCase();
        for (MoveType value : values()) {
            if (value.translate.equalsIgnoreCase(frenchName))
                return value;
        }
        throw new NoSuchElementException("unknown translation for '" + frenchName + "'");
    }

    public boolean winAgainst(MoveType type) {
        int typeWeaknessOrdinal = (type.ordinal() + 1) % values().length;
        return typeWeaknessOrdinal == ordinal();
    }

}
