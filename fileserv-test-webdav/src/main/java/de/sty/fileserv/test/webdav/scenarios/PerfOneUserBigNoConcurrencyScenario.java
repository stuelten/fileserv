package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;
import de.sty.fileserv.test.webdav.utils.DataVerificationUtils;
import de.sty.fileserv.test.webdav.utils.DeterministicInputStream;

import java.util.List;
import java.util.Map;

/**
 * Performance scenario for a single user with big files and no concurrency.
 * Measures sequential upload, download, and deletion of 50MB files.
 */
public class PerfOneUserBigNoConcurrencyScenario implements WebDavScenario {

    @Override
    public String getDescription() { return "Performance test with one user, big files and no concurrency"; }

    @Override
    public List<WebDavTask> prepareTasks(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        List<WebDavTask> tasks = new java.util.ArrayList<>();
        tasks.add(new WebDavTask() {
            @Override
            public String getName() { return "Performance one user big file sequential operations"; }

            @Override
            public void execute(String baseUrl, Sardine sardine, Map<String, String> credentials) throws Exception {
                String testRoot = baseUrl + "perf_one_user_big_no_concurrency_test_root/";
                if (sardine.exists(testRoot)) sardine.delete(testRoot);
                sardine.createDirectory(testRoot);

                long fileSize = 50 * 1024 * 1024; // 50 MB
                long seed = 123L;

                try {
                    for (int i = 0; i < 5; i++) {
                        String fileName = "sequential_big_file_" + i + ".bin";
                        String fileUrl = testRoot + fileName;

                        try (DeterministicInputStream inputStream = new DeterministicInputStream(fileSize, seed)) {
                            sardine.put(fileUrl, inputStream);
                        }

                        // Verify content by reading it back sequentially
                        DataVerificationUtils.verifyContent(fileSize, seed, sardine.get(fileUrl));

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
