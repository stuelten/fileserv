package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scenario simulating a macOS Finder-like flow of WebDAV operations.
 */
public class MacOsFinderFlowScenario implements WebDavScenario {

    @Override
    public String getDescription() {
        return "Simulates macOS Finder flow: PROPFIND Depth:1, GET unknown, PUT, GET, MKCOL, COPY, GET, DELETE";
    }

    @Override
    public List<WebDavTask> prepareTasks(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        List<WebDavTask> tasks = new ArrayList<>();
        tasks.add(new WebDavTask() {
            @Override
            public String getName() {
                return "macOS Finder Flow";
            }

            @Override
            public void execute(String baseUrl, Sardine sardine, Map<String, String> credentials) throws Exception {
                // 1. open a directory (PROPFIND on dataDir, Depth: 1)
                // Sardine.list(url) does a PROPFIND with Depth 1 by default (or Depth 0 if not specified, but sardine.list is Depth 1)
                sardine.list(baseUrl);

                // 2. read a file which should return a 404
                final String PREFIX = (getName() + "_" + System.currentTimeMillis()).replace(" ", "_");
                String fileUrl = baseUrl + PREFIX + ".txt";
                try {
                    sardine.get(fileUrl);
                    throw new RuntimeException("Expected 404 for " + fileUrl);
                } catch (IOException e) {
                    if (!e.getMessage().contains("404")) throw e;
                }

                // 3. create a new test file with content "Hello World!"
                sardine.put(fileUrl, "Hello World!".getBytes());

                // 4. read the test file and check for "Hello World!"
                byte[] content = sardine.get(fileUrl).readAllBytes();
                if (!"Hello World!".equals(new String(content))) {
                    throw new RuntimeException("Content mismatch: " + new String(content));
                }

                // 5. Create a sub-directory "subdir"
                String subDirUrl = baseUrl + PREFIX + "_subdir/";
                if (!sardine.exists(subDirUrl)) {
                    sardine.createDirectory(subDirUrl);
                }

                // 6. Copy "testfile.txt" to "subdir"
                String destinationUrl = subDirUrl + "testfile.txt";
                sardine.copy(fileUrl, destinationUrl);

                // 7. read the copied test file and check for "Hello World!"
                byte[] copiedContent = sardine.get(destinationUrl).readAllBytes();
                if (!"Hello World!".equals(new String(copiedContent))) {
                    throw new RuntimeException("Copied content mismatch: " + new String(copiedContent));
                }

                // 8. Remove all test files and directories
                sardine.delete(destinationUrl);
                sardine.delete(subDirUrl);
                sardine.delete(fileUrl);
            }
        });
        return tasks;
    }
}
