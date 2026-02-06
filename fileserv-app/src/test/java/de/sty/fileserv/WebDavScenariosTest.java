package de.sty.fileserv;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import de.sty.fileserv.core.*;
import de.sty.fileserv.test.webdav.WebDavTestRunner;
import de.sty.fileserv.test.webdav.scenarios.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebDavScenariosTest {

    private final Map<String, String> credentials = new HashMap<>();
    @TempDir
    Path tempDir;
    private Server server;
    private String baseUrl;
    private Sardine sardine;

    @BeforeEach
    void start() throws Exception {
        credentials.put("alice", "secret");
        Authenticator authenticator = new SimpleAuthenticator("alice", "secret");
        InMemoryAuthorizator authorizator = new InMemoryAuthorizator();
        for (Authorizator.Permission p : Authorizator.Permission.values()) {
            authorizator.grant("alice", "/", p);
        }

        server = WebDavServer.build(new FileServConfig(
                new FileStorage(tempDir),
                authorizator,
                false,   // behindProxy
                true,   // allowHttp (for testing)
                0,
                0,
                null, null, null,
                authenticator
        ));

        var connector = new ServerConnector(server);
        connector.setPort(0);
        server.addConnector(connector);

        server.start();
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        baseUrl = "http://localhost:" + port + "/";

        sardine = SardineFactory.begin("alice", "secret");
    }

    @AfterEach
    void stop() throws Exception {
        if (server != null) server.stop();
    }

    @Test
    void runWebDavScenarios() {
        WebDavTestRunner runner = new WebDavTestRunner(baseUrl, sardine, credentials);
        runner.setQuiet(false);
        runner.setVerbose(true);

        List<WebDavScenario> scenarios = Arrays.asList(
                new ShallowCRUDScenario(),
                new MacOsFinderFlowScenario(),
                new WindowsExplorerFlowScenario()
        );

        List<ScenarioResult> results = runner.executeScenarios(scenarios);
        runner.printSummary(results);

        assertThat(results).isNotEmpty();
        for (ScenarioResult result : results) {
            assertThat(result.isSuccess()).as("Scenario %s failed: %s", result.getScenarioName(), result.getMessage()).isTrue();
        }
    }
}
