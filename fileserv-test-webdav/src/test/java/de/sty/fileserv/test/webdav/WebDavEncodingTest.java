package de.sty.fileserv.test.webdav;

import de.sty.fileserv.test.webdav.scenarios.ScenarioUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class WebDavEncodingTest {

    @Test
    void testEncodePath() {
        String original = "special !@#$%^&()_+-={}[];',.txt";
        String encoded = ScenarioUtils.encodePath(original);

        // Verify that the resulting URI is valid
        String fullUrl = "http://localhost:8080/" + encoded;
        URI uri = URI.create(fullUrl);
        assertThat(uri).isNotNull();

        assertThat(encoded).doesNotContain("#");
        assertThat(encoded).contains("%20");
    }

}
