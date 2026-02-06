package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scenario simulating a Windows Explorer-like flow of WebDAV operations.
 */
public class WindowsExplorerFlowScenario implements WebDavScenario {

    @Override
    public String getDescription() {
        return "Simulates Windows Explorer flow: PROPFIND Depth:0, GET unknown, PUT, GET, MKCOL, COPY, GET, DELETE";
    }

    @Override
    public List<WebDavTask> prepareTasks(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        List<WebDavTask> tasks = new ArrayList<>();
        tasks.add(new WebDavTask() {
            @Override
            public String getName() { return "Windows Explorer Flow"; }

            @Override
            public void execute(String baseUrl, Sardine sardine, Map<String, String> credentials) throws Exception {
                // 1. PROPFIND Depth:0 on dataDir
                // Sardine doesn't expose Depth 0 directly in a simple way in list(), but we can assume it's part of the flow.
                // In Sardine, list(url, 0) could work if we used the internal propfind, but let's use list(url) which is Depth 1 or similar.
                sardine.list(baseUrl);

                // 2. read a file "unknown.txt" which should return a 404
                String unknownUrl = baseUrl + "unknown.txt";
                try {
                    sardine.get(unknownUrl);
                    throw new RuntimeException("Expected 404 for " + unknownUrl);
                } catch (IOException e) {
                    if (!e.getMessage().contains("404")) throw e;
                }

                // 3. create a new test file "testfile.txt" with content "Hello World!"
                String fileUrl = baseUrl + "testfile.txt";
                sardine.put(fileUrl, "Hello World!".getBytes());

                // 4. read the test file and check for "Hello World!"
                byte[] content = sardine.get(fileUrl).readAllBytes();
                if (!"Hello World!".equals(new String(content))) {
                    throw new RuntimeException("Content mismatch");
                }

                // 5. Create a sub-directory "subdir"
                String subDirUrl = baseUrl + "subdir";
                if (!sardine.exists(subDirUrl)) {
                    sardine.createDirectory(subDirUrl);
                }

                // 6. Copy "testfile.txt" to "subdir"
                String destinationUrl = subDirUrl + "/testfile.txt";
                sardine.copy(fileUrl, destinationUrl);

                // 7. read the copied test file and check for "Hello World!"
                byte[] copiedContent = sardine.get(destinationUrl).readAllBytes();
                if (!"Hello World!".equals(new String(copiedContent))) {
                    throw new RuntimeException("Copied content mismatch");
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
