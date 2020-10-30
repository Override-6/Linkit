package fr.overridescala.vps.ftp.extension.ppc.logic;

public enum MoveType {

    PIERRE,
    PAPIER,
    CISEAUX;

    public boolean winAgainst(MoveType type) {
        int typeWeaknessOrdinal = (type.ordinal() + 1) % values().length;
        return typeWeaknessOrdinal == ordinal();
    }

}
