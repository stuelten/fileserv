package de.sty.fileserv.test.webdav;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class WebDavEncodingTest {

    @Test
    void testEncodePath() throws Exception {
        WebDavTestTool tool = new WebDavTestTool();
        Method encodePathMethod = WebDavTestTool.class.getDeclaredMethod("encodePath", String.class);
        encodePathMethod.setAccessible(true);

        String original = "special !@#$%^&()_+-={}[];',.txt";
        String encoded = (String) encodePathMethod.invoke(tool, original);

        // Verify that the resulting URI is valid
        String fullUrl = "http://localhost:8080/" + encoded;
        URI uri = URI.create(fullUrl);
        assertThat(uri).isNotNull();

        assertThat(encoded).doesNotContain("#");
        assertThat(encoded).contains("%20");
    }

}
