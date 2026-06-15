package pucmm.freddy.fullstacktesting.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;

/**
 * Steps de integracion para autenticacion y roles. Llaman al backend levantado
 * por @SpringBootTest (puerto aleatorio), que a su vez usa el Keycloak y la BD
 * de docker-compose. Cubren login, endpoint protegido y administracion de roles.
 */
public class RolesSteps {

    /** Roles internos de Keycloak que no son del negocio; se ignoran al comparar. */
    private static final Set<String> BUILTIN = Set.of("offline_access", "uma_authorization");

    private final ObjectMapper mapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    private String token;
    private int lastStatus;
    private String lastBody;

    // ----------------------------------------------------------------- login / me

    @Cuando("inicio sesion con {string} y {string}")
    public void inicioSesion(String username, String password) {
        Resp r = exchange(client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "password", password)));
        lastStatus = r.status();
        lastBody = r.body();
        token = (r.status() == 200) ? readJson(r.body()).get("accessToken").asText() : null;
    }

    @Entonces("el login responde codigo {int}")
    public void elLoginRespondeCodigo(int code) {
        assertThat(lastStatus).isEqualTo(code);
    }

    @Y("recibo un token de acceso")
    public void reciboUnTokenDeAcceso() {
        assertThat(token).isNotBlank();
    }

    @Cuando("consulto mis datos sin token")
    public void consultoMisDatosSinToken() {
        Resp r = exchange(client().get().uri("/api/auth/me"));
        lastStatus = r.status();
        lastBody = r.body();
    }

    @Y("consulto mis datos con el token")
    public void consultoMisDatosConElToken() {
        Resp r = exchange(client().get().uri("/api/auth/me")
                .header("Authorization", "Bearer " + token));
        lastStatus = r.status();
        lastBody = r.body();
    }

    @Entonces("la consulta responde codigo {int}")
    public void laConsultaRespondeCodigo(int code) {
        assertThat(lastStatus).isEqualTo(code);
    }

    @Y("mis roles son {string}")
    public void misRolesSon(String esperados) {
        Set<String> actual = roles(readJson(lastBody).get("roles"));
        assertThat(actual).isEqualTo(parseRoles(esperados));
    }

    // -------------------------------------------------------- administracion de roles

    @Dado("que el usuario {string} no tiene el rol {string}")
    public void aseguraUsuarioSinRol(String username, String role) {
        String id = userId(username);
        if (rolesOfUser(id).contains(role)) {
            exchange(client().delete().uri("/api/admin/users/{id}/roles/{r}", id, role));
        }
        assertThat(rolesOfUser(id)).doesNotContain(role);
    }

    @Cuando("asigno el rol {string} al usuario {string}")
    public void asignoRol(String role, String username) {
        Resp r = exchange(client().post().uri("/api/admin/users/{id}/roles/{r}", userId(username), role));
        assertThat(r.status()).isEqualTo(204);
    }

    @Cuando("quito el rol {string} al usuario {string}")
    public void quitoRol(String role, String username) {
        Resp r = exchange(client().delete().uri("/api/admin/users/{id}/roles/{r}", userId(username), role));
        assertThat(r.status()).isEqualTo(204);
    }

    @Entonces("el usuario {string} tiene el rol {string}")
    public void usuarioTieneRol(String username, String role) {
        assertThat(rolesOfUser(userId(username))).contains(role);
    }

    @Entonces("el usuario {string} no tiene el rol {string}")
    public void usuarioNoTieneRol(String username, String role) {
        assertThat(rolesOfUser(userId(username))).doesNotContain(role);
    }

    // ------------------------------------------------------------------------ helpers

    private String userId(String username) {
        JsonNode users = readJson(exchange(client().get().uri("/api/admin/users")).body());
        for (JsonNode u : users) {
            if (username.equals(u.get("username").asText())) {
                return u.get("id").asText();
            }
        }
        throw new IllegalStateException("Usuario no encontrado: " + username);
    }

    private Set<String> rolesOfUser(String id) {
        Resp r = exchange(client().get().uri("/api/admin/users/{id}", id));
        return roles(readJson(r.body()).get("realmRoles"));
    }

    private Set<String> roles(JsonNode array) {
        Set<String> result = new HashSet<>();
        if (array != null && array.isArray()) {
            for (JsonNode n : array) {
                String name = n.asText();
                if (!BUILTIN.contains(name) && !name.startsWith("default-roles-")) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    private Set<String> parseRoles(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private JsonNode readJson(String body) {
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException("JSON invalido: " + body, e);
        }
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private Resp exchange(RestClient.RequestHeadersSpec<?> spec) {
        // exchange() no lanza excepcion en 4xx/5xx (a diferencia de retrieve()),
        // asi podemos verificar codigos 401/404 directamente.
        return spec.exchange((request, response) ->
                new Resp(response.getStatusCode().value(), response.bodyTo(String.class)));
    }

    private record Resp(int status, String body) {
    }
}
