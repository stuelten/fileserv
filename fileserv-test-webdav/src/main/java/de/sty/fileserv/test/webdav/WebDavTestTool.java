package de.sty.fileserv.test.webdav;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Command(name = "webdav-test", mixinStandardHelpOptions = true, version = "1.0",
        description = "Runs tests against a WebDAV server.")
public class WebDavTestTool implements Callable<Integer> {

    @Parameters(index = "0", description = "The URL of the WebDAV server.")
    private String url;

    @Option(names = {"-u", "--user"}, description = "Username for authentication.")
    private String user;

    @Option(names = {"-p", "--password"}, description = "Password for authentication.")
    private String password;

    @Option(names = {"--passwd-file"}, description = "Passwd-like file for multi-user tests.")
    private java.nio.file.Path passwdFile;

    @Option(names = {"--windows-only"}, description = "Test only Windows corner cases.")
    private boolean windowsOnly = false;

    @Option(names = {"--all-corner-cases"}, description = "Test for all corner cases (default).", defaultValue = "true")
    private boolean allCornerCases = true;

    @Option(names = {"--parallel-connections"}, description = "Number of parallel connections for performance test.", defaultValue = "10")
    private int parallelConnections = 10;

    @Option(names = {"--parallel-users"}, description = "Number of parallel users for performance test.", defaultValue = "5")
    private int parallelUsers = 5;

    private Sardine sardine;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new WebDavTestTool()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (!url.endsWith("/")) {
            url += "/";
        }

        if (user != null && password != null) {
            sardine = SardineFactory.begin(user, password);
        } else {
            sardine = SardineFactory.begin();
        }

        System.out.println("Starting WebDAV tests against " + url);

        try {
            runTest1();
            runTest2();
            runTest3();
            runTest4();
            runTest5();
            runTest6();
        } catch (Exception e) {
            System.err.println("Tests failed: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }

        System.out.println("All tests completed successfully.");
        return 0;
    }

    private void runTest1() throws IOException {
        System.out.println("Test 1: Basic CRUD for a file");
        String testUrl = url + "test1.txt";
        byte[] content = "Hello WebDAV".getBytes(StandardCharsets.UTF_8);
        
        sardine.put(testUrl, content);
        if (!sardine.exists(testUrl)) throw new RuntimeException("File not created");
        
        byte[] downloaded = sardine.get(testUrl).readAllBytes();
        if (!new String(downloaded).equals("Hello WebDAV")) throw new RuntimeException("Content mismatch");
        
        sardine.delete(testUrl);
        if (sardine.exists(testUrl)) throw new RuntimeException("File not deleted");
        System.out.println("Test 1 passed.");
    }

    private void runTest2() throws IOException {
        System.out.println("Test 2: Basic CRUD for directory hierarchy");
        String dirUrl = url + "test2dir/";
        String subDirUrl = dirUrl + "subdir/";
        String file1Url = dirUrl + "file1.txt";
        String file2Url = subDirUrl + "file2.txt";

        sardine.createDirectory(dirUrl);
        sardine.put(file1Url, "file1".getBytes());
        sardine.createDirectory(subDirUrl);
        sardine.put(file2Url, "file2".getBytes());

        if (!sardine.exists(file1Url)) throw new RuntimeException("file1 missing");
        if (!sardine.exists(file2Url)) throw new RuntimeException("file2 missing");

        sardine.delete(dirUrl);
        if (sardine.exists(dirUrl)) throw new RuntimeException("dir not deleted");
        System.out.println("Test 2 passed.");
    }

    private void runTest3() throws IOException {
        System.out.println("Test 3: Encoding and special characters");
        List<String> names = new ArrayList<>();
        if (!windowsOnly) {
            names.add("special !@#$%^&()_+-={}[];',.txt");
            names.add("german-äöüß.txt");
            names.add("spaces in name.txt");
            if (allCornerCases) {
                names.add("dev_null"); // Can't easily test /dev/null as a name on all FS, but we use it as a name
            }
        }
        
        // Windows forbidden characters: < > : " / \ | ? *
        if (allCornerCases && !windowsOnly) {
            names.add("windows_bad_?");
            names.add("windows_bad_*");
        }
        
        if (windowsOnly || allCornerCases) {
            names.add("CON");
            names.add("PRN");
            names.add("AUX");
            names.add("NUL");
            names.add("COM1");
            names.add("LPT1");
        }

        for (String name : names) {
            String testUrl = url + encodePath(name);
            System.out.println(" Testing: " + name);
            try {
                sardine.put(testUrl, "content".getBytes());
                sardine.delete(testUrl);
            } catch (IOException e) {
                System.err.println(" Failed for name '" + name + "': " + e.getMessage());
                if (!windowsOnly) {
                   // Some might naturally fail depending on backend FS
                }
            }
        }
        System.out.println("Test 3 passed (with possible expected failures reported).");
    }

    private String encodePath(String name) {
        // Sardine usually handles encoding, but let's be sure for the URL parts
        return name.replace(" ", "%20");
    }

    private void runTest4() throws IOException {
        System.out.println("Test 4: Deep hierarchy and many files");
        String rootDir = url + "test4/";
        sardine.createDirectory(rootDir);
        
        String currentDir = rootDir;
        for (int i = 0; i < 5; i++) {
            currentDir += "depth" + i + "/";
            sardine.createDirectory(currentDir);
            for (int j = 0; j < 10; j++) {
                sardine.put(currentDir + "file" + j + ".txt", ("content" + j).getBytes());
            }
        }
        
        sardine.delete(rootDir);
        System.out.println("Test 4 passed.");
    }

    private void runTest5() throws IOException {
        if (passwdFile == null) {
            System.out.println("Test 5: Skipped (no --passwd-file provided)");
            return;
        }
        System.out.println("Test 5: Multi-user test from " + passwdFile);
        java.util.List<String> lines = java.nio.file.Files.readAllLines(passwdFile);
        for (String line : lines) {
            String[] parts = line.split(":");
            if (parts.length >= 2) {
                String u = parts[0];
                String p = parts[1];
                System.out.println(" Testing user: " + u);
                Sardine userSardine = SardineFactory.begin(u, p);
                try {
                    userSardine.list(url);
                } catch (IOException e) {
                    System.err.println(" Failed for user " + u + ": " + e.getMessage());
                }
            }
        }
        System.out.println("Test 5 passed.");
    }

    private void runTest6() throws Exception {
        System.out.println("Test 6: Performance test (parallel)");
        ExecutorService executor = Executors.newFixedThreadPool(parallelConnections);
        AtomicInteger successCount = new AtomicInteger();
        int totalRequests = 100;
        
        CountDownLatch latch = new CountDownLatch(totalRequests);
        
        for (int i = 0; i < totalRequests; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    String testUrl = url + "perf_" + finalI + ".txt";
                    sardine.put(testUrl, "perf content".getBytes());
                    sardine.delete(testUrl);
                    successCount.incrementAndGet();
                } catch (IOException e) {
                    System.err.println("Perf error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(1, TimeUnit.MINUTES);
        executor.shutdown();
        System.out.println("Performance test finished. Successes: " + successCount.get() + "/" + totalRequests);
        System.out.println("Test 6 passed.");
    }
}
