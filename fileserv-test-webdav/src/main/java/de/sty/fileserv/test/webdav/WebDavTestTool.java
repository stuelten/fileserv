package de.sty.fileserv.test.webdav;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import de.sty.fileserv.test.webdav.scenarios.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main entry point for the WebDAV testing tool.
 * Executes a series of functional and performance test scenarios against a target WebDAV server.
 * Supports results logging in CSV format and comparison with previous test runs.
 */
@SuppressWarnings("ClassHasNoToStringMethod")
@Command(name = "webdav-test", mixinStandardHelpOptions = true, version = "1.0", description = "Runs tests against a WebDAV server.")
public class WebDavTestTool implements Callable<Integer> {

    private final Map<String, String> credentials = new LinkedHashMap<>();
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;
    @Parameters(index = "0", description = "The URL of the WebDAV server.")
    private String url;
    @Option(names = {"-u", "--user"}, description = "Username for authentication.")
    private String user;
    @Option(names = {"-p", "--password"}, description = "Password for authentication.")
    private String password;
    @Option(names = "--passwd-file", description = "Passwd-like file for multi-user tests.")
    private java.nio.file.Path passwdFile;
    @Option(names = "--windows-only", description = "Test only Windows corner cases.")
    @SuppressWarnings("FieldCanBeLocal")
    private boolean windowsOnly = false;
    @Option(names = "--all-corner-cases", description = "Test for all corner cases (default).", defaultValue = "true")
    @SuppressWarnings("FieldCanBeLocal")
    private boolean allCornerCases = true;
    @Option(names = "--parallel-connections", description = "Number of parallel connections for performance test.", defaultValue = "10")
    @SuppressWarnings("FieldCanBeLocal")
    private int parallelConnections = 10;
    @Option(names = "--parallel-users", description = "Number of parallel users for performance test.", defaultValue = "5")
    private int parallelUsers = 5;
    @Option(names = {"-q", "--quiet"}, description = "Minimize output for successful tests.")
    private boolean quiet = false;
    @Option(names = {"-v", "--verbose"}, description = "Show more detailed output.")
    private boolean verbose = false;
    @Option(names = "--results-dir", description = "Directory to save results.", defaultValue = "test-results")
    private Path resultsDir = Paths.get("test-results");
    @Option(names = "--compare-with", description = "Previous results file to compare with.")
    private Path compareWith;
    private Sardine sardine;

    /**
     * Standard main method to launch the tool.
     *
     * @param args CLI arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new WebDavTestTool()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Main logic execution of the command-line tool.
     *
     * @return exit code (0 for success, 1 for failure)
     */
    @Override
    public Integer call() {
        if (!url.endsWith("/")) {
            url += "/";
        }

        // Initialize primary Sardine client
        if (user != null && password != null) {
            sardine = SardineFactory.begin(user, password);
            credentials.put(user, password);
        } else {
            sardine = SardineFactory.begin();
        }

        // Load additional credentials for multi-user scenarios
        if (passwdFile != null && Files.exists(passwdFile)) {
            try {
                List<String> lines = Files.readAllLines(passwdFile);
                for (String line : lines) {
                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        credentials.put(parts[0], parts[1]);
                    }
                }
            } catch (IOException e) {
                error("Failed to read passwd file: " + e.getMessage());
            }
        }

        log(verbose, "Starting WebDAV tests against " + url);

        // List of scenarios to execute
        List<WebDavScenario> scenarios = Arrays.asList(
                new ShallowCRUDScenario(),
                new DeepCRUDScenario(),
                new BigFileCRUDScenario(),
                new TwoUsersPermissionsScenario(),
                new MultiUserACLScenario(),
                new PerfOneUserShallowScenario(),
                new PerfMultiUserShallowScenario(),
                new PerfOneUserBigNoConcurrencyScenario(),
                new PerfOneUserBigConcurrencyScenario(),
                new PerfMultiUserBigConcurrencyScenario()
        );

        // Execute scenarios
        List<ScenarioResult> results = executeScenarios(scenarios);

        // Reporting and persistence
        printSummary(results);
        saveResults(results);

        // Comparison with historical data
        if (compareWith != null || resultsDir != null) {
            Path toCompare = compareWith;
            if (toCompare == null) {
                toCompare = findLatestResult(url);
            }
            if (toCompare != null && Files.exists(toCompare)) {
                compareResults(results, toCompare);
            }
        }

        boolean allSuccess = results.stream().allMatch(ScenarioResult::isSuccess);
        return allSuccess ? 0 : 1;
    }

    /**
     * Executes the collected scenarios, separating sequential and concurrent ones.
     *
     * @param scenarios list of scenarios to run
     * @return list of results
     */
    private List<ScenarioResult> executeScenarios(List<WebDavScenario> scenarios) {
        List<ScenarioResult> results = Collections.synchronizedList(new ArrayList<>());

        for (WebDavScenario scenario : scenarios) {
            log(!quiet, "\nScenario: " + scenario.getName() + " - " + scenario.getDescription());
            try {
                List<WebDavTask> tasks = scenario.prepareTasks(url, sardine, credentials);
                log(!quiet, "  " + tasks.size() + " tasks will run.");

                long start = System.currentTimeMillis();
                ScenarioResult scenarioResult;
                if (scenario.isConcurrent() && tasks.size() > 1) {
                    scenarioResult = executeConcurrentTasks(scenario, tasks);
                } else {
                    scenarioResult = executeSequentialTasks(scenario, tasks);
                }
                long duration = System.currentTimeMillis() - start;

                // Re-create scenario result to ensure duration reflects the whole run (preparation + tasks + cleanup)
                // But we need to keep the tasks results
                ScenarioResult finalResult = new ScenarioResult(scenario.getName(), scenarioResult.isSuccess(),
                        scenarioResult.isSuccess() ? "Success" : scenarioResult.getMessage(), duration);
                for (WebDavTaskResult tr : scenarioResult.getTaskResults()) {
                    finalResult.addTaskResult(tr);
                }
                results.add(finalResult);

                scenario.cleanup(url, sardine, credentials);
            } catch (Exception e) {
                error("Failed to execute scenario " + scenario.getName() + ": " + e.getMessage());
                results.add(new ScenarioResult(scenario.getName(), false, "Preparation/Cleanup failed: " + e.getMessage(), 0));
            }
        }

        return new ArrayList<>(results);
    }

    private ScenarioResult executeSequentialTasks(WebDavScenario scenario, List<WebDavTask> tasks) {
        boolean allSuccess = true;
        String firstError = "Success";
        long totalDuration = 0;
        List<WebDavTaskResult> taskResults = new ArrayList<>();

        int total = tasks.size();
        for (int i = 0; i < total; i++) {
            WebDavTask task = tasks.get(i);
            log(!quiet, String.format("  [%d/%d] Running task: %s (still waiting: %d)", i + 1, total, task.getName(), total - (i + 1)));

            long start = System.currentTimeMillis();
            try {
                task.execute(url, sardine, credentials);
                long duration = System.currentTimeMillis() - start;
                WebDavTaskResult tr = new WebDavTaskResult(task.getName(), true, null, duration);
                taskResults.add(tr);
                totalDuration += duration;
                log(verbose, String.format("  [%d/%d] Task %s: SUCCESS (%d ms)", i + 1, total, task.getName(), duration));
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - start;
                allSuccess = false;
                if ("Success".equals(firstError)) firstError = e.getMessage();
                WebDavTaskResult tr = new WebDavTaskResult(task.getName(), false, e.getMessage(), duration);
                taskResults.add(tr);
                totalDuration += duration;
                error(String.format("  [%d/%d] Task %s: FAILED (%d ms) - %s", i + 1, total, task.getName(), duration, e.getMessage()));
            }
        }

        ScenarioResult result = new ScenarioResult(scenario.getName(), allSuccess, firstError, totalDuration);
        for (WebDavTaskResult tr : taskResults) result.addTaskResult(tr);
        return result;
    }

    private ScenarioResult executeConcurrentTasks(WebDavScenario scenario, List<WebDavTask> tasks) {
        int maxThreads = Math.min(tasks.size(), scenario.getConcurrencyLevel());
        log(!quiet, "  Executing " + tasks.size() + " tasks concurrently with " + maxThreads + " threads.");

        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        List<WebDavTaskResult> taskResults = Collections.synchronizedList(new ArrayList<>());
        java.util.concurrent.atomic.AtomicInteger completedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        int total = tasks.size();

        long start = System.currentTimeMillis();
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (WebDavTask task : tasks) {
                futures.add(CompletableFuture.runAsync(() -> {
                    long tStart = System.currentTimeMillis();
                    try {
                        task.execute(url, sardine, credentials);
                        long tDuration = System.currentTimeMillis() - tStart;
                        taskResults.add(new WebDavTaskResult(task.getName(), true, null, tDuration));
                    } catch (Exception e) {
                        long tDuration = System.currentTimeMillis() - tStart;
                        taskResults.add(new WebDavTaskResult(task.getName(), false, e.getMessage(), tDuration));
                    } finally {
                        int completed = completedCount.incrementAndGet();
                        if (!quiet) {
                            log(true, String.format("  Progress: %d/%d tasks completed (%d still waiting)", completed, total, total - completed));
                        }
                    }
                }, executor));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executor.shutdown();
        }
        long duration = System.currentTimeMillis() - start;

        boolean allSuccess = taskResults.stream().allMatch(WebDavTaskResult::isSuccess);
        String message = allSuccess ? "Success" : taskResults.stream()
                .filter(r -> !r.isSuccess())
                .map(WebDavTaskResult::getErrorMessage)
                .findFirst()
                .orElse("Unknown error");

        ScenarioResult result = new ScenarioResult(scenario.getName(), allSuccess, message, duration);
        for (WebDavTaskResult tr : taskResults) result.addTaskResult(tr);
        return result;
    }


    /**
     * Prints a summary table of all executed scenarios to the console.
     *
     * @param results list of scenario results
     */
    private void printSummary(List<ScenarioResult> results) {
        log(!quiet, "\n--- Test Summary ---");
        for (ScenarioResult r : results) {
            log(!quiet, String.format("%-30s | %-10s | %d ms", r.getScenarioName(), r.isSuccess() ? "SUCCESS" : "FAILED", r.getDurationMs()));
            if (verbose || !r.isSuccess()) {
                for (WebDavTaskResult tr : r.getTaskResults()) {
                    log(!quiet, String.format("  - %-26s | %-10s | %d ms %s",
                            tr.getTaskName(),
                            tr.isSuccess() ? "SUCCESS" : "FAILED",
                            tr.getDurationMs(),
                            tr.isSuccess() ? "" : "(" + tr.getErrorMessage() + ")"));
                }
            }
        }
        log(!quiet, "--------------------\n");
    }

    /**
     * Saves the results of the test run to a CSV file.
     *
     * @param results list of scenario results
     */
    private void saveResults(List<ScenarioResult> results) {
        try {
            Files.createDirectories(resultsDir);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String hostPart = url.replaceAll("[^a-zA-Z0-9]", "_");
            String filename = String.format("results_%s_%s.csv", hostPart, timestamp);
            Path filePath = resultsDir.resolve(filename);

            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(filePath))) {
                writer.println("Scenario,Success,DurationMs,Message,TargetURL");
                for (ScenarioResult r : results) {
                    writer.printf("%s,%b,%d,\"%s\",\"%s\"\n",
                            r.getScenarioName(), r.isSuccess(), r.getDurationMs(),
                            r.getMessage().replace("\"", "'"), url);
                }
            }
            log(!quiet, "Results saved to " + filePath);
        } catch (IOException e) {
            error("Failed to save results: " + e.getMessage());
        }
    }

    /**
     * Finds the most recent result CSV file for the given URL.
     *
     * @param targetUrl the target URL used in the test run
     * @return path to the latest results file, or null if none found
     */
    private Path findLatestResult(String targetUrl) {
        if (!Files.exists(resultsDir)) return null;
        try {
            String hostPart = targetUrl.replaceAll("[^a-zA-Z0-9]", "_");
            return Files.list(resultsDir)
                    .filter(p -> p.getFileName().toString().startsWith("results_" + hostPart))
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .max(Comparator.comparing(Path::getFileName))
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Compares the current run's results with data from a previous CSV file and prints the difference.
     *
     * @param currentResults results of the current run
     * @param previousFile   path to the previous results file
     */
    private void compareResults(List<ScenarioResult> currentResults, Path previousFile) {
        log(!quiet, "\n--- Comparison with " + previousFile.getFileName() + " ---");
        try {
            List<String> lines = Files.readAllLines(previousFile);
            Map<String, Long> previousDurations = new HashMap<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",");
                if (parts.length >= 3) {
                    previousDurations.put(parts[0], Long.parseLong(parts[2]));
                }
            }

            log(!quiet, String.format("%-30s | %-12s | %-12s | %-10s", "Scenario", "Current", "Previous", "Diff"));
            for (ScenarioResult r : currentResults) {
                Long prev = previousDurations.get(r.getScenarioName());
                String diffStr = "N/A";
                if (prev != null) {
                    long diff = r.getDurationMs() - prev;
                    diffStr = String.format("%+d ms", diff);
                }
                log(!quiet, String.format("%-30s | %8d ms | %8s | %s",
                        r.getScenarioName(), r.getDurationMs(), (prev != null ? prev + " ms" : "N/A"), diffStr));
            }
            log(!quiet, "------------------------------------------\n");
        } catch (Exception e) {
            error("Failed to compare results: " + e.getMessage());
        }
    }

    /**
     * Helper to log messages to the standard output.
     *
     * @param print if true, the message is printed
     * @param msg   message to print
     */
    private void log(boolean print, String msg) {
        if (print) {
            if (spec != null) {
                spec.commandLine().getOut().println(msg);
            } else {
                System.out.println(msg);
            }
        }
    }

    /**
     * Helper to log error messages to the standard error.
     *
     * @param msg error message to print
     */
    private void error(String msg) {
        if (spec != null) {
            spec.commandLine().getErr().println(msg);
        } else {
            System.err.println(msg);
        }
    }

}
