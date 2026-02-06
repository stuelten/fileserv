package de.sty.fileserv.test.webdav.scenarios;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of a WebDAV scenario execution.
 * Contains status information, error messages, and performance metrics.
 */
public class ScenarioResult {
    private final String scenarioName;
    private final boolean success;
    private final String message;
    private final long durationMs;
    private final Map<String, Object> metrics = new LinkedHashMap<>();
    private final List<WebDavTaskResult> taskResults = new ArrayList<>();

    /**
     * Constructs a new ScenarioResult.
     *
     * @param scenarioName the name of the executed scenario
     * @param success      true if the scenario completed successfully
     * @param message      a status or error message
     * @param durationMs   execution time in milliseconds
     */
    public ScenarioResult(String scenarioName, boolean success, String message, long durationMs) {
        this.scenarioName = scenarioName;
        this.success = success;
        this.message = message;
        this.durationMs = durationMs;
    }

    public void addTaskResult(WebDavTaskResult taskResult) {
        taskResults.add(taskResult);
    }

    public List<WebDavTaskResult> getTaskResults() {
        return Collections.unmodifiableList(taskResults);
    }

    /**
     * @return the name of the scenario
     */
    public String getScenarioName() { return scenarioName; }

    /**
     * @return true if successful
     */
    public boolean isSuccess() { return success; }

    /**
     * @return status or error message
     */
    public String getMessage() { return message; }

    /**
     * @return execution duration in milliseconds
     */
    public long getDurationMs() { return durationMs; }

    /**
     * @return a map of additional metrics collected during the scenario
     */
    public Map<String, Object> getMetrics() { return metrics; }

    /**
     * Adds an additional metric to the result.
     *
     * @param key   metric name
     * @param value metric value
     */
    public void addMetric(String key, Object value) {
        metrics.put(key, value);
    }

}
