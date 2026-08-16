package dam.guildmaster.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void corsAllowsOnlyConfiguredOrigins() {
        CorsConfigurationSource source = new SecurityConfig()
                .corsConfigurationSource("https://app.example.test,http://localhost:8082");

        CorsConfiguration configuration = source.getCorsConfiguration(request("/api/users"));

        assertTrue(configuration.getAllowedOrigins().contains("https://app.example.test"));
        assertTrue(configuration.getAllowedOrigins().contains("http://localhost:8082"));
        assertFalse(configuration.getAllowedOrigins().contains("https://attacker.example"));
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
