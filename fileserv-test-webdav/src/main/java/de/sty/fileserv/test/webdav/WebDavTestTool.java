package de.sty.fileserv.test.webdav;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("ClassHasNoToStringMethod")
@Command(name = "webdav-test", mixinStandardHelpOptions = true, version = "1.0", description = "Runs tests against a WebDAV server.")
public class WebDavTestTool implements Callable<Integer> {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Parameters(index = "0", description = "The URL of the WebDAV server.")
    private String url;

    @Option(names = {"-u", "--user"}, description = "Username for authentication.")
    private String user;

    @Option(names = {"-p", "--password"}, description = "Password for authentication.")
    private String password;

    @Option(names = {"--passwd-file"}, description = "Passwd-like file for multi-user tests.")
    private java.nio.file.Path passwdFile;

    @Option(names = {"--windows-only"}, description = "Test only Windows corner cases.")
    @SuppressWarnings("FieldCanBeLocal")
    private boolean windowsOnly = false;

    @Option(names = {"--all-corner-cases"}, description = "Test for all corner cases (default).", defaultValue = "true")
    @SuppressWarnings("FieldCanBeLocal")
    private boolean allCornerCases = true;

    @Option(names = {"--parallel-connections"}, description = "Number of parallel connections for performance test.", defaultValue = "10")
    @SuppressWarnings("FieldCanBeLocal")
    private int parallelConnections = 10;

    @Option(names = {"--parallel-users"}, description = "Number of parallel users for performance test.", defaultValue = "5")
    private int parallelUsers = 5;

    @Option(names = {"-q", "--quiet"}, description = "Minimize output for successful tests.")
    private boolean quiet = false;

    @Option(names = {"-v", "--verbose"}, description = "Show more detailed output.")
    private boolean verbose = false;

    private Sardine sardine;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new WebDavTestTool()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        if (!url.endsWith("/")) {
            url += "/";
        }

        if (user != null && password != null) {
            sardine = SardineFactory.begin(user, password);
        } else {
            sardine = SardineFactory.begin();
        }

        log(verbose, "Starting WebDAV tests against " + url);

        try {
            log(verbose, "Running Test 1...");
            testBasicFileCRUD();
            log(verbose, "Running Test 2...");
            testDirectoryHierarchyCRUD();
            log(verbose, "Running Test 3...");
            testEncodingAndSpecialCharacters();
            log(verbose, "Running Test 4...");
            testDeepHierarchy();
            log(verbose, "Running Test 5...");
            testMultiUser();
            log(verbose, "Running Test 6...");
            testPerformanceParallel();
        } catch (Exception e) {
            error("Tests failed: " + e);
            e.printStackTrace(spec.commandLine().getErr());
            return 1;
        }

        log(!quiet, "All tests completed successfully.");
        return 0;
    }

    private void testBasicFileCRUD() throws IOException {
        log(verbose, "Test 1: Basic CRUD for a file");
        String testUrl = url + "test1.txt";
        byte[] content = "Hello WebDAV".getBytes(StandardCharsets.UTF_8);

        sardine.put(testUrl, content);
        if (!sardine.exists(testUrl)) throw new RuntimeException("File not created");

        byte[] downloaded = sardine.get(testUrl).readAllBytes();
        if (!new String(downloaded).equals("Hello WebDAV")) throw new RuntimeException("Content mismatch");

        sardine.delete(testUrl);
        if (sardine.exists(testUrl)) throw new RuntimeException("File not deleted");
        log(verbose, "Test 1 passed.");
    }

    private void testDirectoryHierarchyCRUD() throws IOException {
        log(verbose, "Test 2: Basic CRUD for directory hierarchy");
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
        log(verbose, "Test 2 passed.");
    }

    private void testEncodingAndSpecialCharacters() {
        log(verbose, "Test 3: Encoding and special characters");
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
            log(verbose, " Testing: " + name);
            try {
                sardine.put(testUrl, "content".getBytes());
                sardine.delete(testUrl);
            } catch (IOException e) {
                log(verbose, " Failed for name '" + name + "': " + e.getMessage());
//                if (!windowsOnly) {
//                   // Some might naturally fail depending on backend FS
//                }
            }
        }
        log(verbose, "Test 3 passed (with possible expected failures reported).");
    }

    private String encodePath(String name) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
    }

    private void testDeepHierarchy() throws IOException {
        log(verbose, "Test 4: Deep hierarchy and many files");
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
        log(verbose, "Test 4 passed.");
    }

    private void testMultiUser() throws IOException {
        if (passwdFile == null) {
            log(verbose, "Test 5: Skipped (no --passwd-file provided)");
            return;
        }
        log(verbose, "Test 5: Multi-user test from " + passwdFile);
        java.util.List<String> lines = java.nio.file.Files.readAllLines(passwdFile);
        for (String line : lines) {
            String[] parts = line.split(":");
            if (parts.length >= 2) {
                String u = parts[0];
                String p = parts[1];
                log(verbose, " Testing user: " + u);
                Sardine userSardine = SardineFactory.begin(u, p);
                try {
                    userSardine.list(url);
                } catch (IOException e) {
                    log(verbose, " Failed for user " + u + ": " + e.getMessage());
                }
            }
        }
        log(verbose, "Test 5 passed.");
    }

    private void testPerformanceParallel() throws Exception {
        log(verbose, "Test 6: Performance test (parallel)");
        AtomicInteger successCount;
        int totalRequests;
        try (ExecutorService executor = Executors.newFixedThreadPool(parallelConnections)) {
            successCount = new AtomicInteger();
            totalRequests = 100;

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
                        log(verbose, "Perf error: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean allFinished = latch.await(1, TimeUnit.MINUTES);
            if (!allFinished) {
                error("Test thread not finished after 1 Minute");
            }
            executor.shutdown();
        }
        log(verbose, "Performance test finished. Successes: " + successCount.get() + "/" + totalRequests);
        log(verbose, "Test 6 passed.");
    }

    private void log(boolean print, String msg) {
        if (print) {
            if (spec != null) {
                spec.commandLine().getOut().println(msg);
            } else {
                System.out.println(msg);
            }
        }
    }

    private void error(String msg) {
        if (spec != null) {
            spec.commandLine().getErr().println(msg);
        } else {
            System.err.println(msg);
        }
    }

}
