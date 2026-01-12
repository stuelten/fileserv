package de.sty.fileserv.core;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static de.sty.fileserv.core.WebDavConstants.HEADER_SERVER;
import static org.assertj.core.api.Assertions.assertThat;

class FileServVersionFilterTest {

    @Test
    void shouldAddServerHeader() throws Exception {
        FileServVersionFilter filter = new FileServVersionFilter();
        ServletRequest request = new ServletStubs.StubServletRequest();
        
        AtomicReference<String> capturedHeader = new AtomicReference<>();
        HttpServletResponse response = new ServletStubs.StubHttpServletResponse() {
            @Override
            public void setHeader(String name, String value) {
                if (HEADER_SERVER.equalsIgnoreCase(name)) {
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

}
