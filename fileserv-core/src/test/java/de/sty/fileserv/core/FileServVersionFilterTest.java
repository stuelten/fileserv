package de.sty.fileserv.core;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FileServVersionFilterTest {

    @Test
    void shouldAddServerHeader() throws Exception {
        FileServVersionFilter filter = new FileServVersionFilter();
        ServletRequest request = new StubServletRequest();
        
        AtomicReference<String> capturedHeader = new AtomicReference<>();
        HttpServletResponse response = new StubHttpServletResponse() {
            @Override
            public void setHeader(String name, String value) {
                if (FileServVersionFilter.HEADER_NAME.equalsIgnoreCase(name)) {
                    capturedHeader.set(value);
                }
            }
        };
        
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        String expectedHeader = FileServVersion.NAME + "/" + FileServVersion.VERSION;
        assertThat(capturedHeader.get()).isEqualTo(expectedHeader);
        assertThat(chainCalled.get()).isTrue();
    }

    // Minimal stubs needed for testing
    static class StubServletRequest implements ServletRequest {
        public Object getAttribute(String name) { return null; }
        public java.util.Enumeration<String> getAttributeNames() { return null; }
        public String getCharacterEncoding() { return null; }
        public void setCharacterEncoding(String env) throws java.io.UnsupportedEncodingException {}
        public int getContentLength() { return 0; }
        public long getContentLengthLong() { return 0; }
        public String getContentType() { return null; }
        public ServletInputStream getInputStream() throws IOException { return null; }
        public String getParameter(String name) { return null; }
        public java.util.Enumeration<String> getParameterNames() { return null; }
        public String[] getParameterValues(String name) { return null; }
        public java.util.Map<String, String[]> getParameterMap() { return null; }
        public String getProtocol() { return null; }
        public String getScheme() { return null; }
        public String getServerName() { return null; }
        public int getServerPort() { return 0; }
        public java.io.BufferedReader getReader() throws IOException { return null; }
        public String getRemoteAddr() { return null; }
        public String getRemoteHost() { return null; }
        public void setAttribute(String name, Object o) {}
        public void removeAttribute(String name) {}
        public java.util.Locale getLocale() { return null; }
        public java.util.Enumeration<java.util.Locale> getLocales() { return null; }
        public boolean isSecure() { return false; }
        public RequestDispatcher getRequestDispatcher(String path) { return null; }
        @Deprecated public String getRealPath(String path) { return null; }
        public int getRemotePort() { return 0; }
        public String getLocalName() { return null; }
        public String getLocalAddr() { return null; }
        public int getLocalPort() { return 0; }
        public ServletContext getServletContext() { return null; }
        public AsyncContext startAsync() throws IllegalStateException { return null; }
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException { return null; }
        public boolean isAsyncStarted() { return false; }
        public boolean isAsyncSupported() { return false; }
        public AsyncContext getAsyncContext() { return null; }
        public DispatcherType getDispatcherType() { return null; }
        public String getRequestId() { return null; }
        public String getProtocolRequestId() { return null; }
        public ServletConnection getServletConnection() { return null; }
    }

    static class StubHttpServletResponse implements HttpServletResponse {
        public void addCookie(jakarta.servlet.http.Cookie cookie) {}
        public boolean containsHeader(String name) { return false; }
        public String encodeURL(String url) { return null; }
        public String encodeRedirectURL(String url) { return null; }
        @Deprecated public String encodeUrl(String url) { return null; }
        @Deprecated public String encodeRedirectUrl(String url) { return null; }
        public void sendError(int sc, String msg) throws IOException {}
        public void sendError(int sc) throws IOException {}
        public void sendRedirect(String location) throws IOException {}
        public void setDateHeader(String name, long date) {}
        public void addDateHeader(String name, long date) {}
        public void setHeader(String name, String value) {}
        public void addHeader(String name, String value) {}
        public void setIntHeader(String name, int value) {}
        public void addIntHeader(String name, int value) {}
        public void setStatus(int sc) {}
        @Deprecated public void setStatus(int sc, String sm) {}
        public int getStatus() { return 0; }
        public String getHeader(String name) { return null; }
        public java.util.Collection<String> getHeaders(String name) { return null; }
        public java.util.Collection<String> getHeaderNames() { return null; }
        public String getCharacterEncoding() { return null; }
        public String getContentType() { return null; }
        public ServletOutputStream getOutputStream() throws IOException { return null; }
        public java.io.PrintWriter getWriter() throws IOException { return null; }
        public void setCharacterEncoding(String charset) {}
        public void setContentLength(int len) {}
        public void setContentLengthLong(long len) {}
        public void setContentType(String type) {}
        public void setBufferSize(int size) {}
        public int getBufferSize() { return 0; }
        public void flushBuffer() throws IOException {}
        public void resetBuffer() {}
        public boolean isCommitted() { return false; }
        public void reset() {}
        public void setLocale(java.util.Locale loc) {}
        public java.util.Locale getLocale() { return null; }
    }
}
