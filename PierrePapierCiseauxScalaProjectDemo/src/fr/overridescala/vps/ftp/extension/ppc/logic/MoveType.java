package fr.overridescala.vps.ftp.extension.ppc.logic;

import java.util.NoSuchElementException;

public enum MoveType {

    ROCK("PIERRE"),
    PAPER("PAPIER"),
    SCISSORS("CISEAUX");

    private final String translate;

    MoveType(String translate) {
        this.translate = translate;
    }

    public String getTranslate() {
        return translate;
    }

    public static MoveType valueOfFrench(String frenchName) {
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
