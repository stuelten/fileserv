package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;
import de.sty.fileserv.test.webdav.utils.DeterministicInputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Performance scenario for multiple users with big files and concurrency.
 * Simulates multiple users simultaneously uploading and deleting large files.
 */
public class PerfMultiUserBigConcurrencyScenario implements WebDavScenario {

    @Override
    public String getDescription() { return "Performance test with multiple users, big files and concurrency"; }

    @Override
    public boolean isConcurrent() { return true; }

    @Override
    public int getConcurrencyLevel() { return 5; }

    @Override
    public List<WebDavTask> prepareTasks(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        String testRoot = ScenarioUtils.ensureTrailingSlash(baseUrl) + "perf_multi_user_big_concurrency_test_root/";
        if (adminSardine.exists(testRoot)) adminSardine.delete(testRoot);
        adminSardine.createDirectory(testRoot);

        long fileSize = 10 * 1024 * 1024; // 10 MB
        long seed = 999L;

        List<WebDavTask> tasks = new ArrayList<>();
        for (String username : credentials.keySet()) {
            tasks.add(new WebDavTask() {
                @Override
                public String getName() {
                    return "User: " + username;
                }

                @Override
                public void execute(String baseUrl, Sardine sardine, Map<String, String> creds) throws Exception {
                    Sardine userSardine = ScenarioUtils.getSardine(username, creds);
                    for (int i = 0; i < 4; i++) {
                        String fileName = username + "_multi_user_big_" + i + ".bin";
                        String fileUrl = testRoot + fileName;
                        try (DeterministicInputStream inputStream = new DeterministicInputStream(fileSize, seed)) {
                            userSardine.put(fileUrl, inputStream);
                        }
                        userSardine.delete(fileUrl);
                    }
                }
            });
        }
        return tasks;
    }

    @Override
    public void cleanup(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        String testRoot = ScenarioUtils.ensureTrailingSlash(baseUrl) + "perf_multi_user_big_concurrency_test_root/";
        if (adminSardine.exists(testRoot)) {
            adminSardine.delete(testRoot);
        }
    }

}
