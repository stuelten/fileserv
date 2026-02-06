package de.sty.fileserv.test.webdav.scenarios;

/**
 * Represents the result of a single WebDAV task execution.
 */
public class WebDavTaskResult {
    private final String taskName;
    private final boolean success;
    private final String errorMessage;
    private final long durationMs;

    public WebDavTaskResult(String taskName, boolean success, String errorMessage, long durationMs) {
        this.taskName = taskName;
        this.success = success;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
    }

    public String getTaskName() { return taskName; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public long getDurationMs() { return durationMs; }
}
