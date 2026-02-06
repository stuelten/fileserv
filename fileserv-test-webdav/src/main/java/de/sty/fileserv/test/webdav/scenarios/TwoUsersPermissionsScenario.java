package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;
import java.util.List;
import java.util.Map;

/**
 * Scenario that tests basic multi-user permissions.
 * Verifies that two different users can interact with the server.
 * User A writes a file, and User B attempts to read it and write their own file.
 */
public class TwoUsersPermissionsScenario implements WebDavScenario {

    @Override
    public String getDescription() { return "Two users, A writes and reads, B read some, write some"; }

    @Override
    public List<WebDavTask> prepareTasks(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        List<WebDavTask> tasks = new java.util.ArrayList<>();
        tasks.add(new WebDavTask() {
            @Override
            public String getName() { return "Two users permissions operations"; }

            @Override
            public void execute(String baseUrl, Sardine sardine, Map<String, String> credentials) throws Exception {
                if (credentials.size() < 2) {
                    throw new RuntimeException("At least two users required in credentials for TwoUsersPermissionsScenario");
                }

                java.util.List<String> usernames = new java.util.ArrayList<>(credentials.keySet());
                String userAlice = usernames.get(0);
                String userBob = usernames.get(1);

                Sardine sardineAlice = ScenarioUtils.getSardine(userAlice, credentials);
                Sardine sardineBob = ScenarioUtils.getSardine(userBob, credentials);

                String testRoot = baseUrl + "two_users_permissions_test/";
                if (sardine.exists(testRoot)) sardine.delete(testRoot);
                sardine.createDirectory(testRoot);

                try {
                    // Alice writes a file
                    String fileAlice = testRoot + "alice_file.txt";
                    byte[] aliceContent = "Content created by Alice".getBytes();
                    sardineAlice.put(fileAlice, aliceContent);
                    if (!sardineAlice.exists(fileAlice)) {
                        throw new RuntimeException("Alice failed to create her file");
                    }

                    // Bob tries to read Alice's file
                    try {
                        byte[] readByBob = sardineBob.get(fileAlice).readAllBytes();
                        de.sty.fileserv.test.webdav.utils.DataVerificationUtils.verifyContent(aliceContent, readByBob);
                    } catch (com.github.sardine.impl.SardineException e) {
                         // It's acceptable if the server configuration prevents Bob from reading Alice's file
                         // depending on the default ACLs of the target server.
                    }

                    // Bob writes his own file
                    String fileBob = testRoot + "bob_file.txt";
                    byte[] bobContent = "Content created by Bob".getBytes();
                    sardineBob.put(fileBob, bobContent);
                    if (!sardineBob.exists(fileBob)) {
                        throw new RuntimeException("Bob failed to create his file");
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
