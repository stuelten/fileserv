package de.sty.fileserv.core;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import jakarta.servlet.http.Cookie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;

public class ServletStubs {

    @SuppressWarnings({"RedundantThrows", "unused"})
    public static class StubServletRequest implements ServletRequest {
        public Object getAttribute(String name) { return null; }
        public Enumeration<String> getAttributeNames() { return null; }
        public String getCharacterEncoding() { return null; }
        public void setCharacterEncoding(String env) throws UnsupportedEncodingException {}
        public int getContentLength() { return 0; }
        public long getContentLengthLong() { return 0; }
        public String getContentType() { return null; }
        public ServletInputStream getInputStream() throws IOException { return null; }
        public String getParameter(String name) { return null; }
        public Enumeration<String> getParameterNames() { return null; }
        public String[] getParameterValues(String name) { return null; }
        public Map<String, String[]> getParameterMap() { return null; }
        public String getProtocol() { return null; }
        public String getScheme() { return null; }
        public String getServerName() { return null; }
        public int getServerPort() { return 0; }
        public BufferedReader getReader() throws IOException { return null; }
        public String getRemoteAddr() { return null; }
        public String getRemoteHost() { return null; }
        public void setAttribute(String name, Object o) {}
        public void removeAttribute(String name) {}
        public Locale getLocale() { return null; }
        public Enumeration<Locale> getLocales() { return null; }
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

    @SuppressWarnings("RedundantThrows")
    public static class StubHttpServletRequest extends StubServletRequest implements HttpServletRequest {
        private boolean secure;
        private final Map<String, String> headers = new HashMap<>();

        public void setSecure(boolean secure) { this.secure = secure; }
        @Override public boolean isSecure() { return secure; }
        
        public void setHeader(String name, String value) { headers.put(name, value); }
        @Override public String getHeader(String name) { return headers.get(name); }

        public String getAuthType() { return null; }
        public Cookie[] getCookies() { return null; }
        public long getDateHeader(String name) { return 0; }
        public Enumeration<String> getHeaders(String name) { return null; }
        public Enumeration<String> getHeaderNames() { return null; }
        public int getIntHeader(String name) { return 0; }
        public HttpServletMapping getHttpServletMapping() { return null; }
        public String getMethod() { return null; }
        public String getPathInfo() { return null; }
        public String getPathTranslated() { return null; }
        public String getContextPath() { return null; }
        public String getQueryString() { return null; }
        public String getRemoteUser() { return null; }
        public boolean isUserInRole(String role) { return false; }
        public Principal getUserPrincipal() { return null; }
        public String getRequestedSessionId() { return null; }
        public String getRequestURI() { return null; }
        public StringBuffer getRequestURL() { return null; }
        public String getServletPath() { return null; }
        public HttpSession getSession(boolean create) { return null; }
        public HttpSession getSession() { return null; }
        public String changeSessionId() { return null; }
        public boolean isRequestedSessionIdValid() { return false; }
        public boolean isRequestedSessionIdFromCookie() { return false; }
        public boolean isRequestedSessionIdFromURL() { return false; }
        @Deprecated public boolean isRequestedSessionIdFromUrl() { return false; }
        public boolean authenticate(HttpServletResponse response) throws IOException, ServletException { return false; }
        public void login(String username, String password) throws ServletException {}
        public void logout() throws ServletException {}
        public Collection<Part> getParts() throws IOException, ServletException { return null; }
        public Part getPart(String name) throws IOException, ServletException { return null; }
        public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException { return null; }
    }

    @SuppressWarnings({"unused", "RedundantThrows"})
    public static class StubHttpServletResponse implements HttpServletResponse {
        public void addCookie(Cookie cookie) {}
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
        public Collection<String> getHeaders(String name) { return null; }
        public Collection<String> getHeaderNames() { return null; }
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
        public void setLocale(Locale loc) {}
        public Locale getLocale() { return null; }
    }
}
