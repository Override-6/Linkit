package fr.overridescala.linkkit.server.config;

public enum AmbiguityStrategy {

    REJECT_NEW,
    DISCONNECT_CURRENT,
    DISCONNECT_BOTH,
    CLOSE_SERVER //panic

}
