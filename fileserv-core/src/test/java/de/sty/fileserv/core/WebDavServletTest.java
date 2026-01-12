package de.sty.fileserv.core;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebDavServletTest {

    @TempDir
    Path tempDir;

    private WebDavServlet servlet;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new WebDavServlet();
        TestServletConfig config = new TestServletConfig();
        config.initParams.put(WebDavServlet.DATA_DIR, tempDir.toString());
        servlet.init(config);
    }

    @Test
    void testDoGetNotFound() throws Exception {
        TestHttpServletRequest request = new TestHttpServletRequest();
        TestHttpServletResponse response = new TestHttpServletResponse();

        request.pathInfo = "/nonexistent.txt";
        
        servlet.doGet(request, response);

        assertThat(response.errorStatus).isEqualTo(404);
    }

    @Test
    void testDoPutAndGet() throws Exception {
        TestHttpServletRequest putRequest = new TestHttpServletRequest();
        TestHttpServletResponse putResponse = new TestHttpServletResponse();

        String content = "Hello World";
        putRequest.pathInfo = "/test.txt";
        putRequest.inputStream = new StubServletInputStream(content.getBytes(StandardCharsets.UTF_8));

        servlet.doPut(putRequest, putResponse);

        assertThat(putResponse.status).isEqualTo(201);
        assertThat(Files.exists(tempDir.resolve("test.txt"))).isTrue();
        assertThat(Files.readString(tempDir.resolve("test.txt"))).isEqualTo(content);

        // Now GET it
        TestHttpServletRequest getRequest = new TestHttpServletRequest();
        TestHttpServletResponse getResponse = new TestHttpServletResponse();
        StubServletOutputStream outputStream = new StubServletOutputStream();

        getRequest.pathInfo = "/test.txt";
        getResponse.outputStream = outputStream;

        servlet.doGet(getRequest, getResponse);

        assertThat(getResponse.status).isEqualTo(200);
        assertThat(outputStream.getContent()).isEqualTo(content);
    }

    @Test
    void testDoMkCol() throws Exception {
        TestHttpServletRequest request = new TestHttpServletRequest();
        TestHttpServletResponse response = new TestHttpServletResponse();

        request.pathInfo = "/newdir";

        servlet.doMkCol(request, response);

        assertThat(response.status).isEqualTo(201);
        assertThat(Files.isDirectory(tempDir.resolve("newdir"))).isTrue();
    }

    @Test
    void testDoDelete() throws Exception {
        Path file = tempDir.resolve("todelete.txt");
        Files.writeString(file, "content");

        TestHttpServletRequest request = new TestHttpServletRequest();
        TestHttpServletResponse response = new TestHttpServletResponse();

        request.pathInfo = "/todelete.txt";

        servlet.doDelete(request, response);

        assertThat(response.status).isEqualTo(204);
        assertThat(Files.exists(file)).isFalse();
    }

    // --- Helper classes to avoid Mockito ---

    static class TestServletConfig implements ServletConfig {
        Map<String, String> initParams = new HashMap<>();
        @Override public String getServletName() { return "WebDavServlet"; }
        @Override public ServletContext getServletContext() { return null; }
        @Override public String getInitParameter(String name) { return initParams.get(name); }
        @Override public Enumeration<String> getInitParameterNames() { return java.util.Collections.enumeration(initParams.keySet()); }
    }

    static class TestHttpServletRequest implements HttpServletRequest {
        String pathInfo;
        jakarta.servlet.ServletInputStream inputStream;
        @Override public String getPathInfo() { return pathInfo; }
        @Override public jakarta.servlet.ServletInputStream getInputStream() { return inputStream; }
        @Override public String getHeader(String name) { return null; }
        @Override public String getMethod() { return "GET"; }
        @Override public StringBuffer getRequestURL() { return new StringBuffer("http://localhost/"); }
        
        // Unimplemented methods
        @Override public String getAuthType() { return null; }
        @Override public jakarta.servlet.http.Cookie[] getCookies() { return new jakarta.servlet.http.Cookie[0]; }
        @Override public long getDateHeader(String name) { return 0; }
        @Override public int getIntHeader(String name) { return 0; }
        @Override public Enumeration<String> getHeaders(String name) { return null; }
        @Override public Enumeration<String> getHeaderNames() { return null; }
        @Override public String getPathTranslated() { return null; }
        @Override public String getContextPath() { return ""; }
        @Override public String getQueryString() { return null; }
        @Override public String getRemoteUser() { return null; }
        @Override public boolean isUserInRole(String role) { return false; }
        @Override public java.security.Principal getUserPrincipal() { return null; }
        @Override public String getRequestedSessionId() { return null; }
        @Override public String getRequestURI() { return null; }
        @Override public String getServletPath() { return ""; }
        @Override public jakarta.servlet.http.HttpSession getSession(boolean create) { return null; }
        @Override public jakarta.servlet.http.HttpSession getSession() { return null; }
        @Override public String changeSessionId() { return null; }
        @Override public boolean isRequestedSessionIdValid() { return false; }
        @Override public boolean isRequestedSessionIdFromCookie() { return false; }
        @Override public boolean isRequestedSessionIdFromURL() { return false; }
        @Override public boolean authenticate(HttpServletResponse response) { return false; }
        @Override public void login(String username, String password) {}
        @Override public void logout() {}
        @Override public java.util.Collection<jakarta.servlet.http.Part> getParts() { return null; }
        @Override public jakarta.servlet.http.Part getPart(String name) { return null; }
        @Override public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
        @Override public Object getAttribute(String name) { return null; }
        @Override public Enumeration<String> getAttributeNames() { return null; }
        @Override public String getCharacterEncoding() { return null; }
        @Override public void setCharacterEncoding(String env) {}
        @Override public int getContentLength() { return 0; }
        @Override public long getContentLengthLong() { return 0; }
        @Override public String getContentType() { return null; }
        @Override public String getProtocol() { return null; }
        @Override public String getScheme() { return "http"; }
        @Override public String getServerName() { return "localhost"; }
        @Override public int getServerPort() { return 80; }
        @Override public java.io.BufferedReader getReader() { return null; }
        @Override public String getRemoteAddr() { return null; }
        @Override public String getRemoteHost() { return null; }
        @Override public void setAttribute(String name, Object o) {}
        @Override public void removeAttribute(String name) {}
        @Override public java.util.Locale getLocale() { return null; }
        @Override public Enumeration<java.util.Locale> getLocales() { return null; }
        @Override public boolean isSecure() { return false; }
        @Override public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
        @Override public int getRemotePort() { return 0; }
        @Override public String getLocalName() { return null; }
        @Override public String getLocalAddr() { return null; }
        @Override public int getLocalPort() { return 0; }
        @Override public ServletContext getServletContext() { return null; }
        @Override public jakarta.servlet.AsyncContext startAsync() { return null; }
        @Override public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse) { return null; }
        @Override public boolean isAsyncStarted() { return false; }
        @Override public boolean isAsyncSupported() { return false; }
        @Override public jakarta.servlet.AsyncContext getAsyncContext() { return null; }
        @Override public jakarta.servlet.DispatcherType getDispatcherType() { return null; }
        @Override public String getRequestId() { return null; }
        @Override public String getProtocolRequestId() { return null; }
        @Override public jakarta.servlet.ServletConnection getServletConnection() { return null; }
        @Override public String getParameter(String name) { return null; }
        @Override public Map<String, String[]> getParameterMap() { return null; }
        @Override public Enumeration<String> getParameterNames() { return null; }
        @Override public String[] getParameterValues(String name) { return null; }
    }

    static class TestHttpServletResponse implements HttpServletResponse {
        int status = 200;
        int errorStatus;
        StubServletOutputStream outputStream;
        @Override public void setStatus(int sc) { this.status = sc; }
        @Override public void sendError(int sc) { this.errorStatus = sc; }
        @Override public void sendError(int sc, String msg) { this.errorStatus = sc; }
        @Override public jakarta.servlet.ServletOutputStream getOutputStream() { return outputStream; }
        @Override public PrintWriter getWriter() { return new PrintWriter(new StringWriter()); }
        
        // Unimplemented methods
        @Override public void addCookie(jakarta.servlet.http.Cookie cookie) {}
        @Override public boolean containsHeader(String name) { return false; }
        @Override public String encodeURL(String url) { return url; }
        @Override public String encodeRedirectURL(String url) { return url; }
        @Override public void sendRedirect(String location) {}
        @Override public void setDateHeader(String name, long date) {}
        @Override public void addDateHeader(String name, long date) {}
        @Override public void setHeader(String name, String value) {}
        @Override public void addHeader(String name, String value) {}
        @Override public void setIntHeader(String name, int value) {}
        @Override public void addIntHeader(String name, int value) {}
        @Override public int getStatus() { return status; }
        @Override public String getHeader(String name) { return null; }
        @Override public java.util.Collection<String> getHeaders(String name) { return null; }
        @Override public java.util.Collection<String> getHeaderNames() { return null; }
        @Override public String getCharacterEncoding() { return null; }
        @Override public String getContentType() { return null; }
        @Override public void setCharacterEncoding(String charset) {}
        @Override public void setContentLength(int len) {}
        @Override public void setContentLengthLong(long len) {}
        @Override public void setContentType(String type) {}
        @Override public void setBufferSize(int size) {}
        @Override public int getBufferSize() { return 0; }
        @Override public void flushBuffer() {}
        @Override public void resetBuffer() {}
        @Override public boolean isCommitted() { return false; }
        @Override public void reset() {}
        @Override public void setLocale(java.util.Locale loc) {}
        @Override public java.util.Locale getLocale() { return null; }
    }

    static class StubServletInputStream extends jakarta.servlet.ServletInputStream {
        private final java.io.InputStream in;
        public StubServletInputStream(byte[] data) { this.in = new ByteArrayInputStream(data); }
        @Override public int read() throws java.io.IOException { return in.read(); }
        @Override public boolean isFinished() { return false; }
        @Override public boolean isReady() { return true; }
        @Override public void setReadListener(jakarta.servlet.ReadListener readListener) {}
    }

    static class StubServletOutputStream extends jakarta.servlet.ServletOutputStream {
        private final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        @Override public void write(int b) { out.write(b); }
        @Override public boolean isReady() { return true; }
        @Override public void setWriteListener(jakarta.servlet.WriteListener writeListener) {}
        public String getContent() { return out.toString(StandardCharsets.UTF_8); }
    }
}
