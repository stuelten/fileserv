package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;

import java.util.List;
import java.util.Map;

/**
 * Interface representing a WebDAV test scenario.
 * Each scenario defines a set of operations to perform against a WebDAV server
 * and returns a {@link ScenarioResult} summarizing the outcome.
 */
public interface WebDavScenario {
    /**
     * Returns the unique name of the scenario.
     * Defaults to the simple class name of the implementation.
     *
     * @return scenario name
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Returns a brief description of what the scenario tests.
     *
     * @return scenario description
     */
    String getDescription();

    /**
     * Prepares the scenario and returns a list of tasks to be executed by the test tool.
     *
     * @param baseUrl      the base URL of the WebDAV server
     * @param adminSardine a Sardine instance with administrative or primary user credentials
     * @param credentials  a map of available usernames to their passwords for multi-user tests
     * @return a list of {@link WebDavTask} objects
     * @throws Exception if an unexpected error occurs during preparation
     */
    List<WebDavTask> prepareTasks(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception;

    /**
     * Performs any necessary cleanup after all tasks in the scenario have been executed.
     *
     * @param baseUrl      the base URL
     * @param adminSardine the administrative Sardine instance
     * @param credentials  the credentials
     * @throws Exception if cleanup fails
     */
    default void cleanup(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
    }

    /**
     * Returns whether this scenario is intended to be run concurrently.
     *
     * @return true if it can be run in parallel with other concurrent scenarios
     */
    default boolean isConcurrent() {
        return false;
    }

    /**
     * Returns the suggested number of threads for this scenario if it is concurrent.
     *
     * @return suggested thread count
     */
    default int getConcurrencyLevel() {
        return 1;
    }

}
