package de.sty.fileserv.core;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static de.sty.fileserv.core.WebDavConstants.*;

public final class BasicAuthFilter implements Filter {
    private final Authenticator authenticator;
    private final boolean behindProxy;
    private final boolean allowHttp;

    public BasicAuthFilter(Authenticator authenticator, boolean behindProxy, boolean allowHttp) {
        this.authenticator = authenticator;
        this.behindProxy = behindProxy;
        this.allowHttp = allowHttp;
    }

    private static void challenge(HttpServletResponse res) throws IOException {
        res.setHeader(HEADER_WWW_AUTHENTICATE, AUTH_PREFIX_BASIC + "realm=\"webdav\"");
        res.sendError(SC_401_UNAUTHORIZED);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Require external HTTPS for Basic Auth
        if (!allowHttp && !isExternallySecure(req)) {
            res.sendError(SC_403_FORBIDDEN, "HTTPS required");
            return;
        }

        String auth = req.getHeader(HEADER_AUTHORIZATION);
        if (auth == null || !auth.startsWith(AUTH_PREFIX_BASIC)) {
            challenge(res);
            return;
        }

        String decoded = new String(Base64.getDecoder().decode(auth.substring(AUTH_PREFIX_BASIC.length())), StandardCharsets.UTF_8);
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
            String xfProto = req.getHeader(HEADER_X_FORWARDED_PROTO);
            if (xfProto != null && xfProto.equalsIgnoreCase("https")) return true;

            String forwarded = req.getHeader(HEADER_FORWARDED);
            if (forwarded != null && forwarded.toLowerCase().contains("proto=https"))
                return true;
        }
        return false;
    }
}
