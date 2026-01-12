package de.sty.fileserv;

import de.sty.fileserv.core.Authenticator;
import de.sty.fileserv.core.FileServConfig;
import de.sty.fileserv.core.SimpleAuthenticator;
import de.sty.fileserv.core.WebDavServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static de.sty.fileserv.core.WebDavConstants.AUTH_PREFIX_BASIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpStatus.*;

class HttpsSmokeTest {

    @TempDir
    Path tempDir;

    private Server server;
    private URI base;
    private HttpClient client;
    private String auth;

    private static SSLContext insecureSslContext() throws Exception {
        TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, trustAll, new SecureRandom());
        return ctx;
    }

    private static String generateTemporaryKeystore(Path dir) throws Exception {
        Path ksPath = dir.resolve("test-keystore.p12");
        String[] command = {
                "keytool", "-genkeypair",
                "-alias", "webdav",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-storetype", "PKCS12",
                "-keystore", ksPath.toString(),
                "-storepass", "changeit",
                "-keypass", "changeit",
                "-dname", "CN=localhost",
                "-validity", "1"
        };
        Process process = new ProcessBuilder(command).start();
        if (process.waitFor() != 0) {
            throw new RuntimeException("Failed to generate test keystore");
        }
        return ksPath.toString();
    }

    @BeforeEach
    void start() throws Exception {
        auth = AUTH_PREFIX_BASIC + Base64.getEncoder().encodeToString("alice:secret".getBytes(StandardCharsets.UTF_8));

        String ks = generateTemporaryKeystore(tempDir);
        Authenticator authenticator = new SimpleAuthenticator("alice", "secret");
        server = WebDavServer.build(new FileServConfig(
                tempDir,
                false,     // not behind proxy; we want actual TLS termination in Java
                -1,
                0,
                ks,
                "changeit",
                "changeit",
                authenticator
        ));

        server.start();
        int httpsActualPort = 0;
        for (var connector : server.getConnectors()) {
            if (connector instanceof ServerConnector sc) {
                if (sc.getProtocols().contains("ssl")) {
                    httpsActualPort = sc.getLocalPort();
                    break;
                }
            }
        }

        base = URI.create("https://localhost:" + httpsActualPort + "/");

        client = HttpClient.newBuilder()
                .sslContext(insecureSslContext())
                .build();
    }

    @AfterEach
    void stop() throws Exception {
        if (server != null) server.stop();
    }

    @Test
    void httpsPutAndGetWork() throws Exception {
        var put = HttpRequest.newBuilder(base.resolve("t.txt"))
                .header("Authorization", auth)
                .PUT(HttpRequest.BodyPublishers.ofString("secure"))
                .build();

        var putResp = client.send(put, HttpResponse.BodyHandlers.ofString());
        assertThat(putResp.statusCode()).isIn(CREATED_201, NO_CONTENT_204);

        var get = HttpRequest.newBuilder(base.resolve("t.txt"))
                .header("Authorization", auth)
                .GET()
                .build();

        var getResp = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertThat(getResp.statusCode()).isEqualTo(OK_200);
        assertThat(getResp.body()).isEqualTo("secure");
    }
}
