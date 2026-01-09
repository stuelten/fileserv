package de.sty.fileserv.core;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * A filter that adds a "Server" header to every response using values from a properties file.
 */
public final class FileServVersionFilter implements Filter {

    public static final String HEADER_NAME = "Server";
    private static final String SERVER_HEADER = FileServVersion.NAME + "/" + FileServVersion.VERSION;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader(HEADER_NAME, SERVER_HEADER);
        }
        chain.doFilter(request, response);
    }
}
