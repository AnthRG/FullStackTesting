package pucmm.freddy.fullstacktesting.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import pucmm.freddy.fullstacktesting.AbstractIntegrationTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthEndpointIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void me_sinToken_devuelve401() {
        int status = client().get().uri("/api/auth/me")
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(401);
    }

    @Test
    void me_conTokenDeAdmin_devuelveSusRoles() {
        Map<?, ?> me = client().get().uri("/api/auth/me")
                .header("Authorization", "Bearer " + tokenFor("admin", "admin"))
                .retrieve()
                .body(Map.class);

        assertThat(me.get("username")).isEqualTo("admin");
        assertThat(roles(me))
                .contains("VIEW_ROLES", "EDIT_ROLES", "product:view", "product:manage");
    }

    @Test
    void me_conTokenDeUser1_devuelveSoloSusRoles() {
        Map<?, ?> me = client().get().uri("/api/auth/me")
                .header("Authorization", "Bearer " + tokenFor("user1", "user1"))
                .retrieve()
                .body(Map.class);

        assertThat(roles(me))
                .contains("VIEW_ROLES", "product:view")
                .doesNotContain("EDIT_ROLES", "product:manage");
    }

    @SuppressWarnings("unchecked")
    private List<String> roles(Map<?, ?> me) {
        return (List<String>) me.get("roles");
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }
}
