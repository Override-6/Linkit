package fr.overridescala.vps.ftp.api.task;

public enum TaskType {

    ADDRESS("ADDRESS"),
    FILE_INFO("FILE_INFO"),
    DOWNLOAD("UPLOAD"),
    UPLOAD("DOWNLOAD"),
    DISCONNECT("DISCONNECT");

    private final String[] completers;

    TaskType(String... completers) {
        this.completers = completers;
    }

    public boolean isCompleterOf(TaskType type) {
        for (String completer : completers) {
            if (type.name().equals(completer))
                return true;
        }
        return false;
    }

}
