package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;
import java.util.List;
import java.util.Map;

/**
 * Scenario that tests multiple users with an extended set of ACLs.
 * Each user creates their own directory and puts a file in it.
 * Verifies that the server can handle multiple concurrent-like user interactions.
 */
public class MultiUserACLScenario implements WebDavScenario {

    @Override
    public String getDescription() { return "Multiple users with a extended set of ACLs"; }

    @Override
    public List<WebDavTask> prepareTasks(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        List<WebDavTask> tasks = new java.util.ArrayList<>();
        tasks.add(new WebDavTask() {
            @Override
            public String getName() { return "Multi-user ACL operations"; }

            @Override
            public void execute(String baseUrl, Sardine sardine, Map<String, String> credentials) throws Exception {
                String testRoot = baseUrl + "multi_user_acl_test_root/";
                if (sardine.exists(testRoot)) sardine.delete(testRoot);
                sardine.createDirectory(testRoot);

                try {
                    java.util.List<String> usernames = new java.util.ArrayList<>(credentials.keySet());
                    for (String username : usernames) {
                        Sardine userSardine = ScenarioUtils.getSardine(username, credentials);
                        String userDirPath = testRoot + username + "/";

                        // Admin creates the directory for the user
                        sardine.createDirectory(userDirPath);

                        // User puts a file into their directory
                        String fileName = "user_info.txt";
                        byte[] content = ("Info specifically for " + username).getBytes();
                        userSardine.put(userDirPath + fileName, content);

                        // Verify the user can read it back
                        byte[] readBack = userSardine.get(userDirPath + fileName).readAllBytes();
                        de.sty.fileserv.test.webdav.utils.DataVerificationUtils.verifyContent(content, readBack);
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
