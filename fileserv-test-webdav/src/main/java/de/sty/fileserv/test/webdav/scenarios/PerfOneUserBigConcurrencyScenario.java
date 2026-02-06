package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;
import de.sty.fileserv.test.webdav.utils.DeterministicInputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Performance scenario for a single user with big files and concurrency.
 * Simulates a single user uploading multiple big files simultaneously using multiple connections.
 */
public class PerfOneUserBigConcurrencyScenario implements WebDavScenario {

    @Override
    public String getDescription() { return "Performance test with one user, big files and concurrency"; }

    @Override
    public boolean isConcurrent() { return true; }

    @Override
    public int getConcurrencyLevel() { return 5; }

    @Override
    public List<WebDavTask> prepareTasks(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        String testRoot = ScenarioUtils.ensureTrailingSlash(baseUrl) + "perf_one_user_big_concurrency_test_root/";
        if (adminSardine.exists(testRoot)) adminSardine.delete(testRoot);
        adminSardine.createDirectory(testRoot);

        long fileSize = 20 * 1024 * 1024; // 20 MB
        long seed = 555L;

        List<WebDavTask> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int fileIndex = i;
            tasks.add(new WebDavTask() {
                @Override
                public String getName() {
                    return "Upload file " + fileIndex;
                }

                @Override
                public void execute(String baseUrl, Sardine sardine, Map<String, String> creds) throws Exception {
                    String fileName = "concurrent_file_" + fileIndex + ".bin";
                    String fileUrl = testRoot + fileName;
                    try (DeterministicInputStream inputStream = new DeterministicInputStream(fileSize, seed)) {
                        sardine.put(fileUrl, inputStream);
                    }
                    sardine.delete(fileUrl);
                }
            });
        }
        return tasks;
    }

    @Override
    public void cleanup(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        String testRoot = ScenarioUtils.ensureTrailingSlash(baseUrl) + "perf_one_user_big_concurrency_test_root/";
        if (adminSardine.exists(testRoot)) {
            adminSardine.delete(testRoot);
        }
    }

}
