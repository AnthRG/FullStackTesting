package pucmm.freddy.fullstacktesting.security;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;

import java.text.ParseException;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("security")
class AuthenticationFlowIT extends AbstractSecurityTest {

    // Con bruteForceProtected activo, dos fallos seguidos sobre el mismo usuario lo
    // bloquean 60s. Cada test negativo quema su propio usuario para no arrastrar a
    // los demas: user1 y compania son fixture compartido de toda la suite.
    private static final String USUARIO_BLOQUEABLE = "victima-fuerza-bruta";
    private static final String USUARIO_DESHABILITADO = "usuario-inactivo";
    private static final String USUARIO_CLAVE_MALA = "victima-clave-mala";
    private static final String USUARIO_ENUMERACION = "victima-enumeracion";
    private static final String CLAVE_DE_PRUEBA = "Prueba-123";
    private static final String CLAVE_EQUIVOCADA = "esta-no-es-la-clave";
    private static final int INTENTOS_FALLIDOS = 12;

    @BeforeAll
    static void prepararUsuariosDePrueba() throws Exception {
        crearUsuario(USUARIO_BLOQUEABLE);
        crearUsuario(USUARIO_DESHABILITADO);
        crearUsuario(USUARIO_CLAVE_MALA);
        crearUsuario(USUARIO_ENUMERACION);
    }

    @Test
    void credencialesCorrectas_emitenTokenDelUsuario() throws ParseException {
        TokenResponse respuesta = requestToken(REALM, FRONTEND_CLIENT, "user1", "user1");

        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.accessToken()).isNotBlank();
        assertThat(SignedJWT.parse(respuesta.accessToken()).getJWTClaimsSet()
                .getStringClaim("preferred_username")).isEqualTo("user1");
    }

    @Test
    void claveIncorrecta_noEmiteToken() {
        TokenResponse respuesta = requestToken(REALM, FRONTEND_CLIENT, USUARIO_CLAVE_MALA, CLAVE_EQUIVOCADA);

        assertThat(respuesta.status()).isEqualTo(401);
        assertThat(respuesta.accessToken()).isNull();
        assertThat(respuesta.error()).isEqualTo("invalid_grant");
    }

    @Test
    void usuarioInexistente_respondeIgualQueClaveIncorrecta() {
        TokenResponse existente = requestToken(REALM, FRONTEND_CLIENT, USUARIO_ENUMERACION, CLAVE_EQUIVOCADA);
        TokenResponse inventado = requestToken(REALM, FRONTEND_CLIENT, "no-existe-este-usuario", CLAVE_EQUIVOCADA);

        assertThat(inventado.status())
                .as("un status distinto permite enumerar usuarios validos")
                .isEqualTo(existente.status());
        assertThat(inventado.error()).isEqualTo(existente.error());
        assertThat(inventado.errorDescription()).isEqualTo(existente.errorDescription());
    }

    @Test
    void clienteDesconocido_noEmiteToken() {
        TokenResponse respuesta = requestToken(REALM, "cliente-que-no-existe", "user1", "user1");

        assertThat(respuesta.status()).isIn(400, 401);
        assertThat(respuesta.accessToken()).isNull();
    }

    @Test
    void usuarioDeshabilitado_noPuedeAutenticarse() throws Exception {
        assertThat(requestToken(REALM, FRONTEND_CLIENT, USUARIO_DESHABILITADO, CLAVE_DE_PRUEBA).accessToken())
                .as("control: habilitado y con clave correcta el usuario si entra")
                .isNotNull();

        deshabilitar(USUARIO_DESHABILITADO);
        TokenResponse respuesta = requestToken(REALM, FRONTEND_CLIENT, USUARIO_DESHABILITADO, CLAVE_DE_PRUEBA);

        assertThat(respuesta.status()).isIn(400, 401);
        assertThat(respuesta.accessToken()).isNull();
    }

    @Test
    void intentosFallidosSeguidos_bloqueanTemporalmenteLaCuenta() {
        assertThat(requestToken(REALM, FRONTEND_CLIENT, USUARIO_BLOQUEABLE, CLAVE_DE_PRUEBA).accessToken())
                .as("control: antes de los fallos la cuenta autentica bien")
                .isNotNull();

        for (int intento = 0; intento < INTENTOS_FALLIDOS; intento++) {
            requestToken(REALM, FRONTEND_CLIENT, USUARIO_BLOQUEABLE, CLAVE_EQUIVOCADA);
        }

        TokenResponse conLaClaveCorrecta = requestToken(REALM, FRONTEND_CLIENT, USUARIO_BLOQUEABLE, CLAVE_DE_PRUEBA);

        assertThat(conLaClaveCorrecta.accessToken())
                .as("tras %d fallos seguidos la cuenta deberia quedar bloqueada temporalmente", INTENTOS_FALLIDOS)
                .isNull();
    }

    @Test
    void respuestaDeApiConTokenInvalido_noFiltraDetallesInternos() {
        String cuerpo = client().get().uri("/api/products")
                .header("Authorization", "Bearer token.invalido.aqui")
                .exchange((request, response) -> response.bodyTo(String.class));

        assertThat(cuerpo == null ? "" : cuerpo)
                .doesNotContain("Exception")
                .doesNotContain("pucmm.freddy")
                .doesNotContain("org.springframework.security");
    }

    @Test
    void healthEsPublicoPeroSinDetalles() {
        String cuerpo = client().get().uri("/actuator/health")
                .exchange((request, response) -> response.bodyTo(String.class));

        assertThat(cuerpo).contains("\"status\"");
        assertThat(cuerpo)
                .as("show-details debe seguir en 'never': el estado de la BD no es publico")
                .doesNotContain("components")
                .doesNotContain("PostgreSQL");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/env", "/actuator/beans", "/actuator/loggers", "/actuator/heapdump"})
    void actuatorSensibleNoEstaExpuesto(String path) {
        assertThat(statusOf(HttpMethod.GET, path, null))
                .as("%s sin token no puede responder 200", path)
                .isNotEqualTo(200);
    }

    // El user profile declarativo de Keycloak 26 exige email/nombre/apellido: sin
    // ellos el usuario arrastra la required action VERIFY_PROFILE y no puede
    // autenticarse nunca, lo que haria pasar estos tests por la razon equivocada.
    private static void crearUsuario(String username) throws Exception {
        try {
            execKcadm("create", "users", "-r", REALM,
                    "-s", "username=" + username,
                    "-s", "enabled=true",
                    "-s", "emailVerified=true",
                    "-s", "email=" + username + "@ejemplo.test",
                    "-s", "firstName=Prueba",
                    "-s", "lastName=Seguridad");
        } catch (IllegalStateException e) {
            boolean yaExiste = e.getMessage() != null && e.getMessage().contains("409");
            if (!yaExiste) {
                throw e;
            }
        }
        execKcadm("set-password", "-r", REALM, "--username", username, "--new-password", CLAVE_DE_PRUEBA);
        execKcadm("update", "users/" + idDe(username), "-r", REALM,
                "-s", "enabled=true", "-s", "requiredActions=[]");
    }

    private static void deshabilitar(String username) throws Exception {
        execKcadm("update", "users/" + idDe(username), "-r", REALM, "-s", "enabled=false");
    }

    private static String idDe(String username) throws Exception {
        return execKcadmOut("get", "users", "-r", REALM,
                "-q", "username=" + username, "-q", "exact=true",
                "--fields", "id", "--format", "csv", "--noquotes").trim();
    }
}
