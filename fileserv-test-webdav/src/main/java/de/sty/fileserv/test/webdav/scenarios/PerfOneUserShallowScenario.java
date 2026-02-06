package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;
import java.util.List;
import java.util.Map;

/**
 * Performance scenario for a single user with many small files in a shallow hierarchy.
 * Measures the time taken to perform 100 iterations of PUT, GET, and DELETE operations.
 */
public class PerfOneUserShallowScenario implements WebDavScenario {

    @Override
    public String getDescription() { return "Performance test with one user, small files and shallow hierarchies"; }

    @Override
    public List<WebDavTask> prepareTasks(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        List<WebDavTask> tasks = new java.util.ArrayList<>();
        tasks.add(new WebDavTask() {
            @Override
            public String getName() { return "Performance one user shallow operations"; }

            @Override
            public void execute(String baseUrl, Sardine sardine, Map<String, String> credentials) throws Exception {
                String testRoot = baseUrl + "perf_one_user_shallow_test_root/";
                if (sardine.exists(testRoot)) sardine.delete(testRoot);
                sardine.createDirectory(testRoot);

                try {
                    int fileCount = 100;
                    byte[] smallContent = "Standard small content for performance testing.".getBytes();

                    for (int i = 0; i < fileCount; i++) {
                        String fileName = "perf_file_" + i + ".txt";
                        String fileUrl = testRoot + fileName;

                        sardine.put(fileUrl, smallContent);
                        // Just ensuring we can read it, not full verification for performance tests
                        sardine.get(fileUrl).close();
                        sardine.delete(fileUrl);
                    }
                } finally {
                    if (sardine.exists(testRoot)) {
                        sardine.delete(testRoot);
                    }
                }
            }
        });
        return tasks;
    }
}
