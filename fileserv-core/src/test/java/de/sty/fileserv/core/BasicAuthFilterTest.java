package de.sty.fileserv.core;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static de.sty.fileserv.core.WebDavConstants.AUTH_PREFIX_BASIC;
import static de.sty.fileserv.core.WebDavConstants.SC_401_UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;

class BasicAuthFilterTest {

    @Test
    void shouldRejectNonHttps() throws Exception {
        BasicAuthFilter filter = new BasicAuthFilter((u, p) -> true, false);
        ServletStubs.StubHttpServletRequest request = new ServletStubs.StubHttpServletRequest();
        request.setSecure(false);
        
        AtomicInteger errorCode = new AtomicInteger();
        ServletStubs.StubHttpServletResponse response = new ServletStubs.StubHttpServletResponse() {
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
        BasicAuthFilter filter = new BasicAuthFilter((u, p) -> true, true);
        ServletStubs.StubHttpServletRequest request = new ServletStubs.StubHttpServletRequest();
        request.setSecure(false);
        request.setHeader("X-Forwarded-Proto", "https");
        request.setHeader("Authorization", AUTH_PREFIX_BASIC + Base64.getEncoder().encodeToString("user:pass".getBytes(StandardCharsets.UTF_8)));
        
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        filter.doFilter(request, new ServletStubs.StubHttpServletResponse(), (req, res) -> chainCalled.set(true));
        
        assertThat(chainCalled.get()).isTrue();
    }

    @Test
    void shouldChallengeWhenNoAuthHeader() throws Exception {
        BasicAuthFilter filter = new BasicAuthFilter((u, p) -> true, false);
        ServletStubs.StubHttpServletRequest request = new ServletStubs.StubHttpServletRequest();
        request.setSecure(true);
        
        AtomicInteger errorCode = new AtomicInteger();
        ServletStubs.StubHttpServletResponse response = new ServletStubs.StubHttpServletResponse() {
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
        
        assertThat(errorCode.get()).isEqualTo(SC_401_UNAUTHORIZED);
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        BasicAuthFilter filter = new BasicAuthFilter((u, p) -> false, false);
        ServletStubs.StubHttpServletRequest request = new ServletStubs.StubHttpServletRequest();
        request.setSecure(true);
        request.setHeader("Authorization", AUTH_PREFIX_BASIC + Base64.getEncoder().encodeToString("wrong:pass".getBytes(StandardCharsets.UTF_8)));
        
        AtomicInteger errorCode = new AtomicInteger();
        ServletStubs.StubHttpServletResponse response = new ServletStubs.StubHttpServletResponse() {
            @Override
            public void sendError(int sc) {
                errorCode.set(sc);
            }
        };
        
        filter.doFilter(request, response, (req, res) -> {});
        
        assertThat(errorCode.get()).isEqualTo( SC_401_UNAUTHORIZED);
    }

}
