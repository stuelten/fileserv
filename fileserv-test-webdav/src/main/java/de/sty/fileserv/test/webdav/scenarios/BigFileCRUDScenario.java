package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;
import de.sty.fileserv.test.webdav.utils.DeterministicInputStream;

import java.util.List;
import java.util.Map;

/**
 * Scenario that tests CRUD operations with large files.
 * Verifies that a 10MB file can be uploaded, exists on the server,
 * its content remains intact after download, and it can be deleted.
 */
public class BigFileCRUDScenario implements WebDavScenario {

    @Override
    public String getDescription() { return "One user, CRUD with big files"; }

    @Override
    public List<WebDavTask> prepareTasks(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        List<WebDavTask> tasks = new java.util.ArrayList<>();
        tasks.add(new WebDavTask() {
            @Override
            public String getName() { return "Big file CRUD operations"; }

            @Override
            public void execute(String baseUrl, Sardine sardine, Map<String, String> credentials) throws Exception {
                String testDir = baseUrl + "big_file_test_root/";
                if (sardine.exists(testDir)) sardine.delete(testDir);
                sardine.createDirectory(testDir);

                try {
                    long fileSize = 10 * 1024 * 1024; // 10 MB
                    long seed = 42L;

                    String fileName = "large_test_file.bin";
                    String fileUrl = testDir + fileName;

                    // Upload the large file using a stream
                    try (DeterministicInputStream inputStream = new DeterministicInputStream(fileSize, seed)) {
                        sardine.put(fileUrl, inputStream);
                    }

                    // Verify existence and content
                    if (!sardine.exists(fileUrl)) {
                        throw new RuntimeException("Large file was not created on the server");
                    }

                    de.sty.fileserv.test.webdav.utils.DataVerificationUtils.verifyContent(fileSize, seed, sardine.get(fileUrl));

                } finally {
                    // Cleanup
                    if (sardine.exists(testDir)) {
                        sardine.delete(testDir);
                    }
                }
            }
        });
        return tasks;
    }
}
