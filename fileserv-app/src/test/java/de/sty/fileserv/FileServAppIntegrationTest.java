package de.sty.fileserv;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static de.sty.fileserv.core.WebDavConstants.AUTH_PREFIX_BASIC;
import static de.sty.fileserv.core.WebDavConstants.HEADER_SERVER;
import static org.assertj.core.api.Assertions.assertThat;

class FileServAppIntegrationTest {

    @TempDir
    Path tempDir;

    private FileServApp app;
    private ExecutorService executor;
    private int port;

    @AfterEach
    void tearDown() throws Exception {
        if (app != null) {
            app.stop();
        }
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testAppWithCliArguments() throws Exception {
        app = new FileServApp();
        CommandLine cmd = new CommandLine(app);
        
        // Use random ports
        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            cmd.execute(
                    tempDir.toString(),
                    "--http-port=0",
                    "--https-port=-1",
                    "-u", "testuser",
                    "-p", "testpass"
            );
        });

        // Wait for server to start
        long start = System.currentTimeMillis();
        while (app.getServer() == null || !app.getServer().isStarted()) {
            if (System.currentTimeMillis() - start > 10000) {
                throw new RuntimeException("Server failed to start in 10s");
            }
            Thread.sleep(100);
        }

        Server server = app.getServer();
        port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        URI base = URI.create("http://localhost:" + port + "/");

        HttpClient client = HttpClient.newHttpClient();
        String auth = AUTH_PREFIX_BASIC + Base64.getEncoder().encodeToString("testuser:testpass".getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder(base)
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue(HEADER_SERVER)).isPresent().get().asString().contains("fileserv-core/");
    }

    @Test
    void testAppWithAuthPlugin() throws Exception {
        app = new FileServApp();
        CommandLine cmd = new CommandLine(app);

        // Use random ports
        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            cmd.execute(
                    tempDir.toString(),
                    "--http-port=0",
                    "--https-port=-1",
                    "--auth", "file:path=" + tempDir.resolve("users.txt")
            );
        });

        // Create the users file
        java.nio.file.Files.writeString(tempDir.resolve("users.txt"), "pluginuser:pluginpass");

        // Wait for the server to start
        long start = System.currentTimeMillis();
        while (app.getServer() == null || !app.getServer().isStarted()) {
            if (System.currentTimeMillis() - start > 10000) {
                throw new RuntimeException("Server failed to start in 10s");
            }
            Thread.sleep(100);
        }

        Server server = app.getServer();
        port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        URI base = URI.create("http://localhost:" + port + "/");

        HttpClient client = HttpClient.newHttpClient();
        String auth = AUTH_PREFIX_BASIC + Base64.getEncoder().encodeToString("pluginuser:pluginpass".getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder(base)
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
    }
}
