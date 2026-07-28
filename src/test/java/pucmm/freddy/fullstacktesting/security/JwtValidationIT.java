package pucmm.freddy.fullstacktesting.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("security")
class JwtValidationIT extends AbstractSecurityTest {

    private static final String PROTEGIDO = "/api/products";
    private static final String CLIENTE_AUXILIAR = "otra-app";

    // JwtTimestampValidator de Spring tolera 60s de desfase de reloj por defecto:
    // un token no se rechaza por expirado hasta pasado ese margen.
    private static final long SKEW_SEGUNDOS = 60;

    private static String tokenValido;
    private static JWTClaimsSet claimsReales;

    @BeforeAll
    static void tomarTokenRealDeKeycloak() throws Exception {
        tokenValido = tokenFor("admin", "admin");
        claimsReales = SignedJWT.parse(tokenValido).getJWTClaimsSet();
    }

    @Test
    void tokenEmitidoPorKeycloak_esAceptado() {
        assertThat(statusOf(HttpMethod.GET, PROTEGIDO, tokenValido)).isEqualTo(200);
    }

    @Test
    void sinCabeceraAuthorization_devuelve401() {
        assertThat(statusOf(HttpMethod.GET, PROTEGIDO, null)).isEqualTo(401);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Bearer",
            "Bearer   ",
            "Basic YWRtaW46YWRtaW4=",
            "Token abc",
            "Bearer no.es.un.jwt"
    })
    void cabeceraAuthorizationMalformada_devuelve401(String cabecera) {
        assertThat(statusWithHeader(HttpMethod.GET, PROTEGIDO, cabecera)).isEqualTo(401);
    }

    @Test
    void tokenConFirmaAlterada_devuelve401() {
        assertThat(statusOf(HttpMethod.GET, PROTEGIDO, alterarFirma(tokenValido))).isEqualTo(401);
    }

    @Test
    void tokenSinFirmaConAlgNone_devuelve401() {
        String falsificado = new PlainJWT(vigentes(claimsReales)).serialize();

        assertThat(statusOf(HttpMethod.GET, PROTEGIDO, falsificado)).isEqualTo(401);
    }

    @Test
    void tokenFirmadoHs256ConLaClavePublica_devuelve401() throws Exception {
        RSAKey clavePublica = clavePublicaDeKeycloak();
        SignedJWT falsificado = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(clavePublica.getKeyID()).build(),
                vigentes(claimsReales));
        falsificado.sign(new MACSigner(clavePublica.toRSAPublicKey().getModulus().toByteArray()));

        assertThat(statusOf(HttpMethod.GET, PROTEGIDO, falsificado.serialize())).isEqualTo(401);
    }

    @Test
    void tokenFirmadoConOtraLlave_devuelve401() throws Exception {
        RSAKey llaveIntrusa = new RSAKeyGenerator(2048).keyID("kid-inventado").generate();
        SignedJWT falsificado = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(llaveIntrusa.getKeyID()).build(),
                vigentes(claimsReales));
        falsificado.sign(new RSASSASigner(llaveIntrusa));

        assertThat(statusOf(HttpMethod.GET, PROTEGIDO, falsificado.serialize())).isEqualTo(401);
    }

    @Test
    void tokenDeOtroRealm_devuelve401() {
        TokenResponse master = requestToken("master", "admin-cli", adminUsername(), adminPassword());
        assertThat(master.status()).isEqualTo(200);

        assertThat(statusOf(HttpMethod.GET, PROTEGIDO, master.accessToken())).isEqualTo(401);
    }

    @Test
    void tokenEnQueryParam_noAutentica() {
        int status = client().get()
                .uri(uri -> uri.path(PROTEGIDO).queryParam("access_token", tokenValido).build())
                .exchange((request, response) -> response.getStatusCode().value());

        assertThat(status).isEqualTo(401);
    }

    @Test
    void tokenDeOtroClienteDelRealm_devuelve401() throws Exception {
        crearClienteAuxiliar();
        TokenResponse ajeno = requestToken(REALM, CLIENTE_AUXILIAR, "user1", "user1");
        assertThat(ajeno.status()).isEqualTo(200);

        assertThat(statusOf(HttpMethod.GET, PROTEGIDO, ajeno.accessToken())).isEqualTo(401);
    }

    @Test
    @Tag("slow")
    void tokenExpirado_devuelve401() throws Exception {
        execKcadm("update", "realms/" + REALM, "-s", "accessTokenLifespan=1");
        try {
            String efimero = tokenFor("user1", "user1");
            Date expira = SignedJWT.parse(efimero).getJWTClaimsSet().getExpirationTime();
            esperarHasta(expira.toInstant().plusSeconds(SKEW_SEGUNDOS + 2));

            assertThat(statusOf(HttpMethod.GET, PROTEGIDO, efimero)).isEqualTo(401);
            assertThat(wwwAuthenticateOf(PROTEGIDO, efimero)).contains("invalid_token");
        } finally {
            execKcadm("update", "realms/" + REALM, "-s", "accessTokenLifespan=300");
        }
    }

    private static JWTClaimsSet vigentes(JWTClaimsSet base) {
        Instant ahora = Instant.now();
        return new JWTClaimsSet.Builder(base)
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(ahora.plusSeconds(300)))
                .build();
    }

    private static String alterarFirma(String token) {
        String[] partes = token.split("\\.");
        byte[] firma = new Base64URL(partes[2]).decode();
        firma[0] ^= 0x01;
        return partes[0] + "." + partes[1] + "." + Base64URL.encode(firma);
    }

    private static RSAKey clavePublicaDeKeycloak() throws Exception {
        String jwks = RestClient.create().get().uri(jwkSetUri()).retrieve().body(String.class);
        return JWKSet.parse(jwks).getKeys().stream()
                .filter(RSAKey.class::isInstance)
                .map(RSAKey.class::cast)
                .filter(key -> KeyUse.SIGNATURE.equals(key.getKeyUse()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El JWKS de Keycloak no expone una clave RSA de firma"));
    }

    private static void crearClienteAuxiliar() throws Exception {
        try {
            execKcadm("create", "clients", "-r", REALM,
                    "-s", "clientId=" + CLIENTE_AUXILIAR,
                    "-s", "enabled=true",
                    "-s", "publicClient=true",
                    "-s", "directAccessGrantsEnabled=true");
        } catch (IllegalStateException e) {
            boolean yaExiste = e.getMessage() != null && e.getMessage().contains("409");
            if (!yaExiste) {
                throw e;
            }
        }
    }

    private static void esperarHasta(Instant momento) throws InterruptedException {
        Duration falta = Duration.between(Instant.now(), momento);
        if (!falta.isNegative() && !falta.isZero()) {
            Thread.sleep(falta.toMillis());
        }
    }
}
