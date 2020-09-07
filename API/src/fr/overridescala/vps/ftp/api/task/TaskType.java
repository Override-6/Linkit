package fr.overridescala.vps.ftp.api.task;

public enum TaskType {

    ADDRESS(),
    FILE_INFO(),
    DOWNLOAD("UPLOAD"),
    UPLOAD("DOWNLOAD"),
    INITIALISATION(),
    DISCONNECT();

    private final String[] completers;

    TaskType(String... completers) {
        this.completers = completers;
    }

    public boolean isCompleterOf(TaskType type) {
        if (type == this)
            return true;
        for (String completer : completers) {
            if (type.name().equals(completer))
                return true;
        }
        return false;
    }

}
