package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scenario that performs basic CRUD operations in a shallow directory structure.
 * Tests directory creation, file creation (PUT), existence check, and deletion.
 * Verifies that the uploaded content is correct.
 */
public class ShallowCRUDScenario implements WebDavScenario {

    @Override
    public String getDescription() { return "One user, CRUD operations with a few dirs and files with a shallow hierarchy"; }

    @Override
    public List<WebDavTask> prepareTasks(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        List<WebDavTask> tasks = new ArrayList<>();
        tasks.add(new WebDavTask() {
            @Override
            public String getName() { return "Shallow CRUD operations"; }

            @Override
            public void execute(String baseUrl, Sardine sardine, Map<String, String> credentials) throws Exception {
                String testDir = baseUrl + "shallow_crud_test_root/";
                if (sardine.exists(testDir)) sardine.delete(testDir);
                sardine.createDirectory(testDir);

                try {
                    for (int i = 1; i <= 3; i++) {
                        String fileName = "file_" + i + ".txt";
                        String fileUrl = testDir + fileName;
                        byte[] expectedContent = ("Deterministic content for " + fileName).getBytes();

                        sardine.put(fileUrl, expectedContent);
                        if (!sardine.exists(fileUrl)) throw new RuntimeException("File " + fileName + " not created");

                        byte[] actualContent = sardine.get(fileUrl).readAllBytes();
                        de.sty.fileserv.test.webdav.utils.DataVerificationUtils.verifyContent(expectedContent, actualContent);

                        String subDirName = "subdir_" + i + "/";
                        String subDirUrl = testDir + subDirName;
                        sardine.createDirectory(subDirUrl);

                        String subFileName = "subfile.txt";
                        byte[] subExpectedContent = "subcontent".getBytes();
                        sardine.put(subDirUrl + subFileName, subExpectedContent);

                        byte[] subActualContent = sardine.get(subDirUrl + subFileName).readAllBytes();
                        de.sty.fileserv.test.webdav.utils.DataVerificationUtils.verifyContent(subExpectedContent, subActualContent);
                    }
                } finally {
                    sardine.delete(testDir);
                    if (sardine.exists(testDir)) {
                        //noinspection ThrowFromFinallyBlock
                        throw new RuntimeException("Test directory ‘" + testDir + "' should not exist, but was not deleted!");
                    }
                }
            }
        });
        return tasks;
    }
}
