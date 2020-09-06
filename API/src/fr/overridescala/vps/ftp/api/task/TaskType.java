package fr.overridescala.vps.ftp.api.task;

public enum TaskType {

    ADDRESS("ADDRESS"),
    FILE_INFO("FINFO"),
    DOWNLOAD("UPLOAD"),
    UPLOAD("DOWNLOAD");

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
