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
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Main entry point for the WebDAV testing tool.
 * Executes a series of functional and performance test scenarios against a target WebDAV server.
 * Supports results logging in CSV format and comparison with previous test runs.
 */
@SuppressWarnings("ClassHasNoToStringMethod")
@Command(name = "webdav-test", subcommands = {WebDavTestTool.ListScenarios.class, WebDavTestTool.RunTests.class}, mixinStandardHelpOptions = true, version = "1.1", description = "Runs tests against a WebDAV server.")
public class WebDavTestTool implements Callable<Integer> {

    private final Map<String, String> credentials = new LinkedHashMap<>();
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;
    @Option(names = {"-u", "--username"}, description = "Username to be used for authentication.")
    private String user;
    @Option(names = {"-p", "--password"}, description = "Password for authentication.")
    private String password;
    @Option(names = "--passwd", description = "A file containing \"username:password\" pairs, one per line.")
    private Path passwdFile;
    @Option(names = {"-s", "--server"}, description = "The URL of the server to be tested.", required = true)
    private String url;
    @Option(names = {"-V", "--version"}, versionHelp = true, description = "The version of the tester.")
    private boolean versionInfoRequested;
    @Option(names = {"-v", "--verbose"}, description = "Be verbose.")
    private boolean verbose = false;
    @Option(names = {"-q", "--quiet"}, description = "Be quiet.")
    private boolean quiet = false;
    @Option(names = "--results-dir", description = "Directory to save results.", defaultValue = "test-results", hidden = true)
    private Path resultsDir = Paths.get("test-results");
    @Option(names = "--compare-with", description = "Previous results file to compare with.", hidden = true)
    private Path compareWith;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new WebDavTestTool()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    public void list() {
        List<WebDavScenario> scenarios = getAllScenarios();
        spec.commandLine().getOut().println("Available scenarios:");
        for (WebDavScenario scenario : scenarios) {
            spec.commandLine().getOut().printf("  %-40s %s%n", scenario.getName(), scenario.getDescription());
        }
    }

    public Integer test(String... scenarioNames) {
        if (!url.endsWith("/")) {
            url += "/";
        }

        // Initialize primary Sardine client
        Sardine sardine;
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

        WebDavTestRunner runner = new WebDavTestRunner(url, sardine, credentials);
        runner.setQuiet(quiet);
        runner.setVerbose(verbose);
        runner.setResultsDir(resultsDir);
        runner.setOutputWriters(new PrintWriter(spec.commandLine().getOut(), true), new PrintWriter(spec.commandLine().getErr(), true));

        log(verbose, "Starting WebDAV tests against " + url);

        List<WebDavScenario> allScenarios = getAllScenarios();
        List<WebDavScenario> toRun;

        if (scenarioNames == null || scenarioNames.length == 0 || (scenarioNames.length == 1 && "all".equalsIgnoreCase(scenarioNames[0]))) {
            toRun = allScenarios;
        } else {
            toRun = new ArrayList<>();
            Set<String> requested = new HashSet<>(Arrays.asList(scenarioNames));
            for (WebDavScenario s : allScenarios) {
                if (requested.contains(s.getName())) {
                    toRun.add(s);
                    requested.remove(s.getName());
                }
            }
            if (!requested.isEmpty()) {
                error("Unknown scenarios: " + requested);
                return 1;
            }
        }

        // Execute scenarios
        List<ScenarioResult> results = runner.executeScenarios(toRun);

        // Reporting and persistence
        runner.printSummary(results);
        runner.saveResults(results);

        // Comparison with historical data
        if (compareWith != null || resultsDir != null) {
            Path toCompare = compareWith;
            if (toCompare == null) {
                toCompare = runner.findLatestResult();
            }
            if (toCompare != null && Files.exists(toCompare)) {
                runner.compareResults(results, toCompare);
            }
        }

        boolean allSuccess = results.stream().allMatch(ScenarioResult::isSuccess);
        return allSuccess ? 0 : 1;
    }

    private List<WebDavScenario> getAllScenarios() {
        return Arrays.asList(
                new ShallowCRUDScenario(),
                new DeepCRUDScenario(),
                new BigFileCRUDScenario(),
                new MacOsFinderFlowScenario(),
                new WindowsExplorerFlowScenario(),
                new TwoUsersPermissionsScenario(),
                new MultiUserACLScenario(),
                new PerfOneUserShallowScenario(),
                new PerfMultiUserShallowScenario(),
                new PerfOneUserBigNoConcurrencyScenario(),
                new PerfOneUserBigConcurrencyScenario(),
                new PerfMultiUserBigConcurrencyScenario()
        );
    }

    private void log(boolean print, String msg) {
        if (print) {
            spec.commandLine().getOut().println(msg);
        }
    }

    private void error(String msg) {
        spec.commandLine().getErr().println(msg);
    }

    @Command(name = "list", description = "lists all scenarios.")
    public static class ListScenarios implements Runnable {
        @CommandLine.ParentCommand
        private WebDavTestTool parent;

        @Override
        public void run() {
            parent.list();
        }
    }

    @Command(name = "test", description = "runs the scenarios given.")
    public static class RunTests implements Callable<Integer> {
        @CommandLine.ParentCommand
        private WebDavTestTool parent;

        @Parameters(description = "Scenarios to run. Use 'all' or leave empty to run all scenarios.")
        private String[] scenarioNames;

        @Override
        public Integer call() {
            return parent.test(scenarioNames);
        }
    }
}
