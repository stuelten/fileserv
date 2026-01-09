package de.sty.fileserv.core;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleBasicAuthFilterTest {

    @Test
    void shouldRejectNonHttps() throws Exception {
        SimpleBasicAuthFilter filter = new SimpleBasicAuthFilter((u, p) -> true, false);
        StubHttpServletRequest request = new StubHttpServletRequest();
        request.setSecure(false);
        
        AtomicInteger errorCode = new AtomicInteger();
        StubHttpServletResponse response = new StubHttpServletResponse() {
            @Override
            public void sendError(int sc, String msg) {
                errorCode.set(sc);
            }
        };
        
        filter.doFilter(request, response, (req, res) -> {});
        
        assertThat(errorCode.get()).isEqualTo(403);
    }

    @Test
    void shouldAcceptHttpsViaHeader() throws Exception {
        SimpleBasicAuthFilter filter = new SimpleBasicAuthFilter((u, p) -> true, true);
        StubHttpServletRequest request = new StubHttpServletRequest();
        request.setSecure(false);
        request.setHeader("X-Forwarded-Proto", "https");
        request.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("user:pass".getBytes(StandardCharsets.UTF_8)));
        
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        filter.doFilter(request, new StubHttpServletResponse(), (req, res) -> chainCalled.set(true));
        
        assertThat(chainCalled.get()).isTrue();
    }

    @Test
    void shouldChallengeWhenNoAuthHeader() throws Exception {
        SimpleBasicAuthFilter filter = new SimpleBasicAuthFilter((u, p) -> true, false);
        StubHttpServletRequest request = new StubHttpServletRequest();
        request.setSecure(true);
        
        AtomicInteger errorCode = new AtomicInteger();
        StubHttpServletResponse response = new StubHttpServletResponse() {
            @Override
            public void sendError(int sc) {
                errorCode.set(sc);
            }
            @Override
            public void setHeader(String name, String value) {
                assertThat(name).isEqualTo("WWW-Authenticate");
                assertThat(value).contains("Basic");
            }
        };
        
        filter.doFilter(request, response, (req, res) -> {});
        
        assertThat(errorCode.get()).isEqualTo(401);
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        SimpleBasicAuthFilter filter = new SimpleBasicAuthFilter((u, p) -> false, false);
        StubHttpServletRequest request = new StubHttpServletRequest();
        request.setSecure(true);
        request.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("wrong:pass".getBytes(StandardCharsets.UTF_8)));
        
        AtomicInteger errorCode = new AtomicInteger();
        StubHttpServletResponse response = new StubHttpServletResponse() {
            @Override
            public void sendError(int sc) {
                errorCode.set(sc);
            }
        };
        
        filter.doFilter(request, response, (req, res) -> {});
        
        assertThat(errorCode.get()).isEqualTo(401);
    }

    static class StubHttpServletRequest extends FileServVersionFilterTest.StubServletRequest implements HttpServletRequest {
        private boolean secure;
        private final Map<String, String> headers = new HashMap<>();

        public void setSecure(boolean secure) { this.secure = secure; }
        @Override public boolean isSecure() { return secure; }
        
        public void setHeader(String name, String value) { headers.put(name, value); }
        @Override public String getHeader(String name) { return headers.get(name); }

        public String getAuthType() { return null; }
        public jakarta.servlet.http.Cookie[] getCookies() { return null; }
        public long getDateHeader(String name) { return 0; }
        public java.util.Enumeration<String> getHeaders(String name) { return null; }
        public java.util.Enumeration<String> getHeaderNames() { return null; }
        public int getIntHeader(String name) { return 0; }
        public jakarta.servlet.http.HttpServletMapping getHttpServletMapping() { return null; }
        public String getMethod() { return null; }
        public String getPathInfo() { return null; }
        public String getPathTranslated() { return null; }
        public String getContextPath() { return null; }
        public String getQueryString() { return null; }
        public String getRemoteUser() { return null; }
        public boolean isUserInRole(String role) { return false; }
        public java.security.Principal getUserPrincipal() { return null; }
        public String getRequestedSessionId() { return null; }
        public String getRequestURI() { return null; }
        public StringBuffer getRequestURL() { return null; }
        public String getServletPath() { return null; }
        public jakarta.servlet.http.HttpSession getSession(boolean create) { return null; }
        public jakarta.servlet.http.HttpSession getSession() { return null; }
        public String changeSessionId() { return null; }
        public boolean isRequestedSessionIdValid() { return false; }
        public boolean isRequestedSessionIdFromCookie() { return false; }
        public boolean isRequestedSessionIdFromURL() { return false; }
        @Deprecated public boolean isRequestedSessionIdFromUrl() { return false; }
        public boolean authenticate(HttpServletResponse response) throws IOException, jakarta.servlet.ServletException { return false; }
        public void login(String username, String password) throws jakarta.servlet.ServletException {}
        public void logout() throws jakarta.servlet.ServletException {}
        public java.util.Collection<jakarta.servlet.http.Part> getParts() throws IOException, jakarta.servlet.ServletException { return null; }
        public jakarta.servlet.http.Part getPart(String name) throws IOException, jakarta.servlet.ServletException { return null; }
        public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, jakarta.servlet.ServletException { return null; }
    }
    
    static class StubHttpServletResponse extends FileServVersionFilterTest.StubHttpServletResponse {}
}
