package de.sty.fileserv;

import de.sty.fileserv.core.Authenticator;
import de.sty.fileserv.core.FileServConfig;
import de.sty.fileserv.core.SimpleAuthenticator;
import de.sty.fileserv.core.WebDavServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

import static de.sty.fileserv.core.WebDavConstants.AUTH_PREFIX_BASIC;
import static de.sty.fileserv.core.WebDavConstants.HEADER_SERVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpStatus.*;

class WebDavIntegrationTest {

    @TempDir Path tempDir;

    private Server server;
    private URI base;
    private HttpClient client;
    private String auth;

    @BeforeEach
    void start() throws Exception {
        auth = AUTH_PREFIX_BASIC + Base64.getEncoder().encodeToString("alice:secret".getBytes(StandardCharsets.UTF_8));
        Authenticator authenticator = new SimpleAuthenticator("alice", "secret");
        server = WebDavServer.build(new FileServConfig(
                tempDir,
                true,   // behindProxy
                0,      // httpPort: 0 => let Jetty choose? (we’ll set connector port after start)
                0,
                null, null, null,
                authenticator
        ));

        // Add a connector on port 0 (random)
        var connector = new ServerConnector(server);
        connector.setPort(0);
        server.addConnector(connector);

        server.start();
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        base = URI.create("http://localhost:" + port + "/");

        client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @AfterEach
    void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void serverHeaderIsPresent() throws Exception {
        var get = HttpRequest.newBuilder(base)
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .GET()
                .build();

        var resp = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.headers().firstValue(HEADER_SERVER)).isPresent().get().asString().contains("fileserv-core/");
    }

    @Test
    void putGetPropfindWorks() throws Exception {
        // PUT
        var put = HttpRequest.newBuilder(base.resolve("hello.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .PUT(HttpRequest.BodyPublishers.ofString("hi"))
                .build();

        var putResp = client.send(put, HttpResponse.BodyHandlers.ofString());
        assertThat(putResp.statusCode()).isIn(CREATED_201, NO_CONTENT_204);

        // GET
        var get = HttpRequest.newBuilder(base.resolve("hello.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .GET()
                .build();

        var getResp = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertThat(getResp.statusCode()).isEqualTo(OK_200);
        assertThat(getResp.body()).isEqualTo("hi");

        // PROPFIND depth 0
        var propfind = HttpRequest.newBuilder(base.resolve("hello.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .header("Depth", "0")
                .method("PROPFIND", HttpRequest.BodyPublishers.noBody())
                .build();

        var propResp = client.send(propfind, HttpResponse.BodyHandlers.ofString());
        assertThat(propResp.statusCode()).isEqualTo(MULTI_STATUS_207);
        assertThat(propResp.body()).contains("multistatus").contains("hello.txt");
    }

    @Test
    void lockRequiresTokenForPutAndUnlockReleases() throws Exception {
        URI target = base.resolve("locked.txt");

        // LOCK (create lock on resource)
        var lockReq = HttpRequest.newBuilder(target)
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .header("Depth", "0")
                .header("Timeout", "Second-300")
                .method("LOCK", HttpRequest.BodyPublishers.ofString(
                        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                "<D:lockinfo xmlns:D='DAV:'>\n" +
                                "  <D:lockscope><D:exclusive/></D:lockscope>\n" +
                                "  <D:locktype><D:write/></D:locktype>\n" +
                                "</D:lockinfo>\n"
                ))
                .build();

        var lockResp = client.send(lockReq, HttpResponse.BodyHandlers.ofString());
        assertThat(lockResp.statusCode()).isEqualTo(OK_200);
        String lockTokenHeader = lockResp.headers().firstValue("Lock-Token").orElseThrow();
        // Format: <opaquelocktoken:...>
        String token = lockTokenHeader.trim();
        assertThat(token).startsWith("<opaquelocktoken:");

        // PUT without token => 423
        var putNoToken = HttpRequest.newBuilder(target)
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .PUT(HttpRequest.BodyPublishers.ofString("data"))
                .build();

        var putNoTokenResp = client.send(putNoToken, HttpResponse.BodyHandlers.ofString());
        assertThat(putNoTokenResp.statusCode()).isEqualTo(LOCKED_423);

        // PUT with If header including token => success
        var putWithToken = HttpRequest.newBuilder(target)
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .header("If", "(" + token + ")")
                .PUT(HttpRequest.BodyPublishers.ofString("data"))
                .build();

        var putWithTokenResp = client.send(putWithToken, HttpResponse.BodyHandlers.ofString());
        assertThat(putWithTokenResp.statusCode()).isIn(CREATED_201, NO_CONTENT_204);

        // UNLOCK
        var unlockReq = HttpRequest.newBuilder(target)
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .header("Lock-Token", token)
                .method("UNLOCK", HttpRequest.BodyPublishers.noBody())
                .build();

        var unlockResp = client.send(unlockReq, HttpResponse.BodyHandlers.ofString());
        assertThat(unlockResp.statusCode()).isEqualTo(NO_CONTENT_204);

        // PUT now works without token
        var putAfterUnlock = HttpRequest.newBuilder(target)
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .PUT(HttpRequest.BodyPublishers.ofString("data2"))
                .build();

        var putAfterUnlockResp = client.send(putAfterUnlock, HttpResponse.BodyHandlers.ofString());
        assertThat(putAfterUnlockResp.statusCode()).isIn(CREATED_201, NO_CONTENT_204);
    }

    @Test
    void moveWithProxyPrefixDestinationIsResolvedCorrectly() throws Exception {
        // Create a file
        var put = HttpRequest.newBuilder(base.resolve("a.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .PUT(HttpRequest.BodyPublishers.ofString("x"))
                .build();
        assertThat(client.send(put, HttpResponse.BodyHandlers.ofString()).statusCode()).isIn(CREATED_201, NO_CONTENT_204);

        // MOVE with absolute destination that includes external prefix /webdav
        // and include X-Forwarded-Prefix so servlet strips it.
        String destination = base.resolve("webdav/b.txt").toString(); // absolute URL with prefix in path

        var move = HttpRequest.newBuilder(base.resolve("a.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .header("Destination", destination)
                .header("X-Forwarded-Prefix", "/webdav")
                .method("MOVE", HttpRequest.BodyPublishers.noBody())
                .build();

        var moveResp = client.send(move, HttpResponse.BodyHandlers.ofString());
        assertThat(moveResp.statusCode()).isIn(201, 204);

        // a.txt should be gone
        var getA = HttpRequest.newBuilder(base.resolve("a.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .GET().build();
        assertThat(client.send(getA, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(NOT_FOUND_404);

        // b.txt should exist
        var getB = HttpRequest.newBuilder(base.resolve("b.txt"))
                .header("Authorization", auth)
                .header("X-Forwarded-Proto", "https")
                .GET().build();
        var getBResp = client.send(getB, HttpResponse.BodyHandlers.ofString());
        assertThat(getBResp.statusCode()).isEqualTo(OK_200);
        assertThat(getBResp.body()).isEqualTo("x");
    }
}
