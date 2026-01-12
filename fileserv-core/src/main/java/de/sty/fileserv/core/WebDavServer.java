package de.sty.fileserv.core;

import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

public final class WebDavServer {
    private static final Logger LOG = LoggerFactory.getLogger(WebDavServer.class);

    private WebDavServer() {}

    public static Server build(FileServConfig cfg) {
        LOG.info("Building WebDavServer (behindProxy={}, httpPort={}, httpsPort={})",
                cfg.behindProxy(), cfg.httpPort(), cfg.httpsPort());
        LOG.info("Data directory: {}", cfg.dataDir().toAbsolutePath().normalize());

        Server server = new Server();

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false);
        if (cfg.behindProxy()) {
            LOG.debug("Enabling Forwarded/X-Forwarded-* support (behindProxy=true)");
            httpConfig.addCustomizer(new ForwardedRequestCustomizer());
        }

        if (cfg.httpPort() >= 0) {
            ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
            http.setPort(cfg.httpPort());
            server.addConnector(http);
            LOG.debug("Added HTTP connector on port {}", cfg.httpPort());
        }

        if (cfg.httpsPort() >= 0) {
            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            httpsConfig.addCustomizer(new SecureRequestCustomizer());

            SslContextFactory.Server ssl = new SslContextFactory.Server();
            ssl.setKeyStorePath(cfg.keyStorePath());
            ssl.setKeyStorePassword(cfg.keyStorePassword());
            ssl.setKeyManagerPassword(cfg.keyPassword());
            LOG.debug("Configured SSL with keystore at {} (passwords not logged)", cfg.keyStorePath());

            ServerConnector https = new ServerConnector(
                    server,
                    new SslConnectionFactory(ssl, "http/1.1"),
                    new HttpConnectionFactory(httpsConfig)
            );
            https.setPort(cfg.httpsPort());
            server.addConnector(https);
            LOG.debug("Added HTTPS connector on port {}", cfg.httpsPort());
        }

        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setContextPath("/");

        ctx.addFilter(new FilterHolder(new FileServVersionFilter()),
                "/*", EnumSet.of(DispatcherType.REQUEST));

        ctx.addFilter(new FilterHolder(new BasicAuthFilter(cfg.authenticator(), cfg.behindProxy())),
                "/*", EnumSet.of(DispatcherType.REQUEST));

        ctx.addServlet(WebDavServlet.class, "/*")
                .setInitParameter(WebDavServlet.DATA_DIR, cfg.dataDir().toAbsolutePath().normalize().toString());

        server.setHandler(ctx);
        LOG.info("Servlet context configured at path '/'. WebDAV servlet mounted at '/*'.");
        return server;
    }
}
