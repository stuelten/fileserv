package de.sty.fileserv.core;
    
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
    
import java.io.IOException;

import static de.sty.fileserv.core.WebDavConstants.HEADER_SERVER;

/**
 * A filter that adds a "Server" header to every response using
 * {@link FileServVersion#NAME} and {@link FileServVersion#VERSION}.
 */
public final class FileServVersionFilter implements Filter {

    private static final String HEADER_SERVER_VALUE = FileServVersion.NAME + "/" + FileServVersion.VERSION;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader(HEADER_SERVER, HEADER_SERVER_VALUE);
        }
        chain.doFilter(request, response);
    }
}
