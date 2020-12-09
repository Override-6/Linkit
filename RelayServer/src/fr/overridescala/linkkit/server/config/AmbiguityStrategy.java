package fr.overridescala.linkkit.server.config;

public enum AmbiguityStrategy {

    REJECT_NEW,
    DISCONNECT_OLD,
    DISCONNECT_BOTH,
    ACCEPT_BOTH, //packets would be cloned and sent to both connections
    CLOSE_SERVER //panic

}
