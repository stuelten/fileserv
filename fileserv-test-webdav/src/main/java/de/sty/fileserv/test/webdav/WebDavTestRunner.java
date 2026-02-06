package de.sty.fileserv.test.webdav;

import com.github.sardine.Sardine;
import de.sty.fileserv.test.webdav.scenarios.ScenarioResult;
import de.sty.fileserv.test.webdav.scenarios.WebDavScenario;
import de.sty.fileserv.test.webdav.scenarios.WebDavTask;
import de.sty.fileserv.test.webdav.scenarios.WebDavTaskResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runner for WebDAV test scenarios.
 * Executes a series of functional and performance test scenarios against a target WebDAV server.
 */
public class WebDavTestRunner {

    private final String url;
    private final Sardine sardine;
    private final Map<String, String> credentials;
    private boolean quiet = false;
    private boolean verbose = false;
    private Path resultsDir = Paths.get("test-results");
    private PrintWriter out;
    private PrintWriter err;

    public WebDavTestRunner(String url, Sardine sardine, Map<String, String> credentials) {
        this.url = url.endsWith("/") ? url : url + "/";
        this.sardine = sardine;
        this.credentials = credentials != null ? new LinkedHashMap<>(credentials) : new LinkedHashMap<>();
        this.out = new PrintWriter(System.out, true);
        this.err = new PrintWriter(System.err, true);
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setResultsDir(Path resultsDir) {
        this.resultsDir = resultsDir;
    }

    public void setOutputWriters(PrintWriter out, PrintWriter err) {
        this.out = out;
        this.err = err;
    }

    /**
     * Executes the collected scenarios.
     *
     * @param scenarios list of scenarios to run
     * @return list of results
     */
    public List<ScenarioResult> executeScenarios(List<WebDavScenario> scenarios) {
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
    public void printSummary(List<ScenarioResult> results) {
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
    public void saveResults(List<ScenarioResult> results) {
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
     * @return path to the latest results file, or null if none found
     */
    public Path findLatestResult() {
        if (!Files.exists(resultsDir)) return null;
        try {
            String hostPart = url.replaceAll("[^a-zA-Z0-9]", "_");
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
    public void compareResults(List<ScenarioResult> currentResults, Path previousFile) {
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

    private void log(boolean print, String msg) {
        if (print && out != null) {
            out.println(msg);
        }
    }

    private void error(String msg) {
        if (err != null) {
            err.println(msg);
        }
    }
}
