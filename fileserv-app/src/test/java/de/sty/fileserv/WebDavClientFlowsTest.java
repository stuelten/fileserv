package de.sty.fileserv;

import de.sty.fileserv.core.Authenticator;
import de.sty.fileserv.core.SimpleAuthenticator;
import de.sty.fileserv.core.WebDavServer;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class WebDavClientFlowsTest {

    @TempDir
    Path tempDir;

    private Server server;
    private URI base;
    private HttpClient client;
    private String auth;

    @BeforeEach
    void start() throws Exception {
        auth = "Basic " + Base64.getEncoder().encodeToString("alice:secret".getBytes(StandardCharsets.UTF_8));
        Authenticator authenticator = new SimpleAuthenticator("alice", "secret");
        server = WebDavServer.build(new WebDavServer.Config(
                tempDir,
                true,   // behindProxy
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
        base = URI.create("http://localhost:" + port + "/");

        client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @AfterEach
    void stop() throws Exception {
        if (server != null) server.stop();
    }

    @Test
    void macOsLike_flow_directory_open_404_put_get_mkcol_copy_get_cleanup() throws Exception {
        // 1. open a directory (PROPFIND on root, Depth: 1)
        var propfind = HttpRequest.newBuilder(base)
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .header("Depth", "1")
                .method("PROPFIND", HttpRequest.BodyPublishers.noBody())
                .build();
        var propResp = client.send(propfind, HttpResponse.BodyHandlers.ofString());
        assertThat(propResp.statusCode()).isEqualTo(HttpStatus.MULTI_STATUS_207);

        // 2. read a file "unknown.txt" which should return a 404
        var getUnknown = HttpRequest.newBuilder(base.resolve("unknown.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .GET().build();
        var getUnknownResp = client.send(getUnknown, HttpResponse.BodyHandlers.ofString());
        assertThat(getUnknownResp.statusCode()).isEqualTo(HttpStatus.NOT_FOUND_404);

        // 3. create a new test file "testfile.txt" with content "Hello World!"
        var put = HttpRequest.newBuilder(base.resolve("testfile.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .PUT(HttpRequest.BodyPublishers.ofString("Hello World!"))
                .build();
        var putResp = client.send(put, HttpResponse.BodyHandlers.ofString());
        assertThat(putResp.statusCode()).isIn(HttpStatus.CREATED_201, HttpStatus.NO_CONTENT_204);

        // 4. read the test file and check for "Hello World!"
        var get = HttpRequest.newBuilder(base.resolve("testfile.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .GET().build();
        var getResp = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertThat(getResp.statusCode()).isEqualTo(HttpStatus.OK_200);
        assertThat(getResp.body()).isEqualTo("Hello World!");

        // 5. Create a sub-directory "subdir"
        var mkcol = HttpRequest.newBuilder(base.resolve("subdir/"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .method("MKCOL", HttpRequest.BodyPublishers.noBody())
                .build();
        var mkcolResp = client.send(mkcol, HttpResponse.BodyHandlers.ofString());
        assertThat(mkcolResp.statusCode()).isIn(HttpStatus.CREATED_201, HttpStatus.METHOD_NOT_ALLOWED_405); // 405 if already exists

        // 6. Copy "testfile.txt" to "subdir"
        String destination = base.resolve("subdir/testfile.txt").toString();
        var copy = HttpRequest.newBuilder(base.resolve("testfile.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .header("Destination", destination)
                .header("Overwrite", "T")
                .method("COPY", HttpRequest.BodyPublishers.noBody())
                .build();
        var copyResp = client.send(copy, HttpResponse.BodyHandlers.ofString());
        assertThat(copyResp.statusCode()).isIn(HttpStatus.CREATED_201, HttpStatus.NO_CONTENT_204);

        // 7. read the copied test file and check for "Hello World!"
        var getCopied = HttpRequest.newBuilder(base.resolve("subdir/testfile.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .GET().build();
        var getCopiedResp = client.send(getCopied, HttpResponse.BodyHandlers.ofString());
        assertThat(getCopiedResp.statusCode()).isEqualTo(HttpStatus.OK_200);
        assertThat(getCopiedResp.body()).isEqualTo("Hello World!");

        // 8. Remove all test files and directories
        var delCopied = HttpRequest.newBuilder(base.resolve("subdir/testfile.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .build();
        assertThat(client.send(delCopied, HttpResponse.BodyHandlers.ofString()).statusCode()).isIn(HttpStatus.OK_200, HttpStatus.NO_CONTENT_204);

        var delSubdir = HttpRequest.newBuilder(base.resolve("subdir/"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .build();
        assertThat(client.send(delSubdir, HttpResponse.BodyHandlers.ofString()).statusCode()).isIn(HttpStatus.OK_200, HttpStatus.NO_CONTENT_204);

        var delOriginal = HttpRequest.newBuilder(base.resolve("testfile.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .build();
        assertThat(client.send(delOriginal, HttpResponse.BodyHandlers.ofString()).statusCode()).isIn(HttpStatus.OK_200, HttpStatus.NO_CONTENT_204, HttpStatus.NOT_FOUND_404);
    }

    @Test
    void windowsLike_flow_directory_open_404_put_get_mkcol_copy_get_cleanup() throws Exception {
        // Simulate Windows WebDAV style: often uses PROPFIND Depth: 0 then 1 and COPY with Overwrite: T
        // 1. PROPFIND Depth:0 on root
        var prop0 = HttpRequest.newBuilder(base)
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .header("Depth", "0")
                .method("PROPFIND", HttpRequest.BodyPublishers.noBody())
                .build();
        assertThat(client.send(prop0, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(HttpStatus.MULTI_STATUS_207);

        // 2. GET unknown -> 404
        var getUnknown = HttpRequest.newBuilder(base.resolve("unknown.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .GET().build();
        assertThat(client.send(getUnknown, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(HttpStatus.NOT_FOUND_404);

        // 3. PUT testfile.txt
        var put = HttpRequest.newBuilder(base.resolve("testfile.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .PUT(HttpRequest.BodyPublishers.ofString("Hello World!"))
                .build();
        assertThat(client.send(put, HttpResponse.BodyHandlers.ofString()).statusCode()).isIn(HttpStatus.CREATED_201, HttpStatus.NO_CONTENT_204);

        // 4. GET file
        var get = HttpRequest.newBuilder(base.resolve("testfile.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .GET().build();
        var getResp = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertThat(getResp.statusCode()).isEqualTo(HttpStatus.OK_200);
        assertThat(getResp.body()).isEqualTo("Hello World!");

        // 5. MKCOL subdir
        var mkcol = HttpRequest.newBuilder(base.resolve("subdir"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .method("MKCOL", HttpRequest.BodyPublishers.noBody())
                .build();
        assertThat(client.send(mkcol, HttpResponse.BodyHandlers.ofString()).statusCode()).isIn(HttpStatus.CREATED_201, HttpStatus.METHOD_NOT_ALLOWED_405);

        // 6. COPY to subdir with Overwrite: T
        String destination = base.resolve("subdir/testfile.txt").toString();
        var copy = HttpRequest.newBuilder(base.resolve("testfile.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .header("Destination", destination)
                .header("Overwrite", "T")
                .method("COPY", HttpRequest.BodyPublishers.noBody())
                .build();
        assertThat(client.send(copy, HttpResponse.BodyHandlers.ofString()).statusCode()).isIn(HttpStatus.CREATED_201, HttpStatus.NO_CONTENT_204);

        // 7. GET copied file
        var getCopied = HttpRequest.newBuilder(base.resolve("subdir/testfile.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .GET().build();
        var getCopiedResp = client.send(getCopied, HttpResponse.BodyHandlers.ofString());
        assertThat(getCopiedResp.statusCode()).isEqualTo(HttpStatus.OK_200);
        assertThat(getCopiedResp.body()).isEqualTo("Hello World!");

        // 8. Cleanup
        var delCopied = HttpRequest.newBuilder(base.resolve("subdir/testfile.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .build();
        assertThat(client.send(delCopied, HttpResponse.BodyHandlers.ofString()).statusCode()).isIn(HttpStatus.OK_200, HttpStatus.NO_CONTENT_204);

        var delSubdir = HttpRequest.newBuilder(base.resolve("subdir/"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .build();
        assertThat(client.send(delSubdir, HttpResponse.BodyHandlers.ofString()).statusCode()).isIn(HttpStatus.OK_200, HttpStatus.NO_CONTENT_204);

        var delOriginal = HttpRequest.newBuilder(base.resolve("testfile.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .build();
        assertThat(client.send(delOriginal, HttpResponse.BodyHandlers.ofString()).statusCode()).isIn(HttpStatus.OK_200, HttpStatus.NO_CONTENT_204, HttpStatus.NOT_FOUND_404);
    }
}
