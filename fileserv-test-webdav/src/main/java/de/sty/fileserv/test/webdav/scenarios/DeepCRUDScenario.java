package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;
import java.util.List;
import java.util.Map;

/**
 * Scenario that tests CRUD operations in a deep directory hierarchy.
 * Creates multiple levels of directories and populates them with many files
 * to test the server's ability to handle complex structures and larger numbers of objects.
 */
public class DeepCRUDScenario implements WebDavScenario {

    @Override
    public String getDescription() { return "One user, CRUD with a lot of files and deep hierarchies"; }

    @Override
    public List<WebDavTask> prepareTasks(String baseUrl, Sardine adminSardine, Map<String, String> credentials) throws Exception {
        List<WebDavTask> tasks = new java.util.ArrayList<>();
        tasks.add(new WebDavTask() {
            @Override
            public String getName() { return "Deep CRUD operations"; }

            @Override
            public void execute(String baseUrl, Sardine sardine, Map<String, String> credentials) throws Exception {
                String testRoot = baseUrl + "deep_hierarchy_test_root/";
                if (sardine.exists(testRoot)) sardine.delete(testRoot);
                sardine.createDirectory(testRoot);

                try {
                    String currentPath = testRoot;
                    int depth = 5;
                    int filesPerLevel = 20;

                    for (int level = 0; level < depth; level++) {
                        String dirName = "level_" + level + "/";
                        currentPath += dirName;
                        sardine.createDirectory(currentPath);

                        for (int fileIndex = 0; fileIndex < filesPerLevel; fileIndex++) {
                            String fileName = "file_at_level_" + level + "_index_" + fileIndex + ".txt";
                            String fileUrl = currentPath + fileName;
                            byte[] content = ("Content of file " + fileIndex + " at level " + level).getBytes();
                            sardine.put(fileUrl, content);
                        }
                    }
                } finally {
                    // Recursive deletion of the root directory
                    if (sardine.exists(testRoot)) {
                        sardine.delete(testRoot);
                    }
                }
            }
        });
        return tasks;
    }
}
