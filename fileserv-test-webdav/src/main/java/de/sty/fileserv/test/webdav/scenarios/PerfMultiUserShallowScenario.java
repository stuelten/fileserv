package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Performance scenario for multiple users with small files in a shallow hierarchy.
 * Simulates concurrent access by multiple users.
 */
public class PerfMultiUserShallowScenario implements WebDavScenario {

    @Override
    public String getDescription() { return "Performance test with multiple users, small files and shallow hierarchies"; }

    @Override
    public boolean isConcurrent() { return true; }

    @Override
    public int getConcurrencyLevel() { return 10; }

    @Override
    public List<WebDavTask> prepareTasks(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        String testRoot = ScenarioUtils.ensureTrailingSlash(baseUrl) + "perf_multi_user_shallow_test_root/";
        if (adminSardine.exists(testRoot)) adminSardine.delete(testRoot);
        adminSardine.createDirectory(testRoot);

        List<WebDavTask> tasks = new ArrayList<>();
        byte[] content = "small content for multi-user test".getBytes();
        for (String username : credentials.keySet()) {
            tasks.add(new WebDavTask() {
                @Override
                public String getName() {
                    return "User: " + username;
                }

                @Override
                public void execute(String baseUrl, Sardine sardine, Map<String, String> creds) throws Exception {
                    Sardine userSardine = ScenarioUtils.getSardine(username, creds);
                    for (int i = 0; i < 20; i++) {
                        String fileUrl = testRoot + username + "_perf_file_" + i + ".txt";
                        userSardine.put(fileUrl, content);
                        userSardine.delete(fileUrl);
                    }
                }
            });
        }
        return tasks;
    }

    @Override
    public void cleanup(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        String testRoot = ScenarioUtils.ensureTrailingSlash(baseUrl) + "perf_multi_user_shallow_test_root/";
        if (adminSardine.exists(testRoot)) {
            adminSardine.delete(testRoot);
        }
    }

}
