package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;
import java.util.Map;

/**
 * Represents a single unit of work within a WebDAV test scenario.
 */
public interface WebDavTask {
    /**
     * @return a descriptive name for the task
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Executes the task.
     *
     * @param baseUrl     the base URL of the WebDAV server
     * @param sardine     a Sardine instance to use for the task
     * @param credentials the available credentials
     * @throws Exception  if the task fails
     */
    void execute(String baseUrl, Sardine sardine, Map<String, String> credentials) throws Exception;
}
