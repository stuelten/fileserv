package de.sty.fileserv.core;
    
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
    
import java.io.IOException;

import static de.sty.fileserv.core.WebDavConstants.HEADER_SERVER;

/**
 * A filter that adds a "Server" header to every response using values from a properties file.
 */
public final class FileServVersionFilter implements Filter {

    private static final String SERVER_HEADER = FileServVersion.NAME + "/" + FileServVersion.VERSION;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader(HEADER_SERVER, SERVER_HEADER);
        }
        chain.doFilter(request, response);
    }
}
