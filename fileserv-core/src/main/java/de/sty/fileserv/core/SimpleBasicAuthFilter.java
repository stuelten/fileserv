package de.sty.fileserv.core;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class SimpleBasicAuthFilter implements Filter {
    private final Authenticator authenticator;
    private final boolean behindProxy;

    public SimpleBasicAuthFilter(Authenticator authenticator, boolean behindProxy) {
        this.authenticator = authenticator;
        this.behindProxy = behindProxy;
    }

    private static void challenge(HttpServletResponse res) throws IOException {
        res.setHeader("WWW-Authenticate", "Basic realm=\"webdav\"");
        res.sendError(401);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Require external HTTPS for Basic Auth
        if (!isExternallySecure(req)) {
            res.sendError(403, "HTTPS required");
            return;
        }

        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Basic ")) {
            challenge(res);
            return;
        }

        String decoded = new String(Base64.getDecoder().decode(auth.substring(6)), StandardCharsets.UTF_8);
        int idx = decoded.indexOf(':');
        String u = (idx >= 0) ? decoded.substring(0, idx) : decoded;
        String p = (idx >= 0) ? decoded.substring(idx + 1) : "";

        if (!authenticator.authenticate(u, p)) {
            challenge(res);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isExternallySecure(HttpServletRequest req) {
        // If ForwardedRequestCustomizer is enabled and proxy is trusted,
        // req.isSecure() should already reflect X-Forwarded-Proto.
        if (req.isSecure()) return true;

        if (behindProxy) {
            String xfProto = req.getHeader("X-Forwarded-Proto");
            if (xfProto != null && xfProto.equalsIgnoreCase("https")) return true;

            String forwarded = req.getHeader("Forwarded");
            if (forwarded != null && forwarded.toLowerCase().contains("proto=https"))
                return true;
        }
        return false;
    }
}
