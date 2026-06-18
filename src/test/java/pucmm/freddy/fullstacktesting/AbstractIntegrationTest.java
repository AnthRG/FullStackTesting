package pucmm.freddy.fullstacktesting;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.util.Map;

public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("fullstacktesting")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withReuse(true);

    static final KeycloakContainer KEYCLOAK =
            new KeycloakContainer("quay.io/keycloak/keycloak:26.3")
                    .withRealmImportFile("realm-export.json")
                    .withReuse(true);

    static {
        POSTGRES.start();
        KEYCLOAK.start();
        relaxMasterSslRequirement();
    }

    private static void relaxMasterSslRequirement() {
        try {
            execKcadm("config", "credentials",
                    "--server", "http://localhost:8080",
                    "--realm", "master",
                    "--user", KEYCLOAK.getAdminUsername(),
                    "--password", KEYCLOAK.getAdminPassword());
            execKcadm("update", "realms/master", "-s", "sslRequired=NONE");
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "No se pudo relajar sslRequired del realm master en el Keycloak de test", e);
        }
    }

    private static void execKcadm(String... args) throws IOException, InterruptedException {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "/opt/keycloak/bin/kcadm.sh";
        System.arraycopy(args, 0, cmd, 1, args.length);
        Container.ExecResult result = KEYCLOAK.execInContainer(cmd);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("kcadm fallo (" + String.join(" ", cmd) + "): "
                    + result.getStdout() + result.getStderr());
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        String realm = KEYCLOAK.getAuthServerUrl() + "/realms/fullstacktesting";
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> realm);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> realm + "/protocol/openid-connect/certs");
        registry.add("keycloak.admin.server-url", KEYCLOAK::getAuthServerUrl);
    }

    protected static String tokenFor(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "frontend");
        form.add("username", username);
        form.add("password", password);
        Map<?, ?> res = RestClient.create().post()
                .uri(KEYCLOAK.getAuthServerUrl() + "/realms/fullstacktesting/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        return (String) res.get("access_token");
    }
}
