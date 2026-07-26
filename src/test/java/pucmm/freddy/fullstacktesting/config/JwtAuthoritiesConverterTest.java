package pucmm.freddy.fullstacktesting.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica como se traducen los roles del JWT de Keycloak a authorities de Spring Security.
 *
 * <p>Es el punto mas delicado del modelo de permisos: {@code hasRole("X")} es azucar
 * sintactico de {@code hasAuthority("ROLE_X")}, asi que si el converter solo emitiera
 * la forma prefijada, todos los {@code @PreAuthorize("hasAuthority('product:view')")}
 * devolverian 403.
 */
class JwtAuthoritiesConverterTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    @DisplayName("cada rol del realm produce una authority con su nombre tal cual, sin prefijo")
    void cadaRolProduceSuAuthoritySinPrefijo() {
        Collection<String> authorities = authoritiesOf("product:view");

        // Sin "ROLE_" el metodo hasRole() deja de funcionar, y eso es justo lo que se busca:
        // obliga a que toda la autorizacion pase por hasAuthority('<permiso>').
        assertThat(authorities)
                .containsExactly("product:view")
                .doesNotContain("ROLE_product:view");
    }

    @Test
    @DisplayName("Spring Security anade FACTOR_BEARER por su cuenta, ademas de nuestros roles")
    void springAnadeLaAuthorityDelFactorDeAutenticacion() {
        // Spring Security 7 (Spring Boot 4) marca con que factor se autentico el usuario
        // para poder exigir MFA en endpoints sensibles. No sale del realm ni de nuestro
        // converter: la anade el framework. Por eso el resto de los tests la filtra.
        var todas = config.jwtAuthenticationConverter()
                .convert(jwtBuilder().claim("realm_access", Map.of("roles", List.of("product:view"))).build())
                .getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(todas).contains("FACTOR_BEARER");
    }

    @Test
    @DisplayName("los permisos que Keycloak expande de un composite llegan como authorities propias")
    void permisosExpandidosDeUnCompositeLleganComoAuthorities() {
        // Esto es lo que Keycloak mete en el token de 'operator': el rol de negocio
        // mas los permisos que agrupa, ya expandidos.
        Collection<String> authorities = authoritiesOf(
                "INVENTORY_OPERATOR", "product:view", "product:manage", "stock:view", "stock:manage", "report:view");

        assertThat(authorities)
                .contains("INVENTORY_OPERATOR", "product:manage", "stock:manage", "report:view")
                .doesNotContain("audit:view", "user:manage");
    }

    @Test
    @DisplayName("un token sin el claim realm_access no otorga ninguna authority")
    void sinClaimRealmAccessNoHayAuthorities() {
        Jwt jwt = jwtBuilder().claim("scope", "openid profile").build();

        assertThat(convert(jwt)).isEmpty();
    }

    @Test
    @DisplayName("un realm_access sin la lista de roles no otorga ninguna authority")
    void realmAccessSinRolesNoHayAuthorities() {
        Jwt jwt = jwtBuilder().claim("realm_access", Map.of()).build();

        assertThat(convert(jwt)).isEmpty();
    }

    @Test
    @DisplayName("el principal sale de preferred_username, no del sub")
    void elPrincipalSaleDePreferredUsername() {
        Jwt jwt = jwtBuilder().claim("realm_access", Map.of("roles", List.of("product:view"))).build();

        assertThat(config.jwtAuthenticationConverter().convert(jwt).getName()).isEqualTo("operator");
    }

    private Collection<String> authoritiesOf(String... realmRoles) {
        return convert(jwtBuilder()
                .claim("realm_access", Map.of("roles", List.of(realmRoles)))
                .build());
    }

    /** Authorities que salen del realm, sin las que anade Spring por el factor de autenticacion. */
    private Collection<String> convert(Jwt jwt) {
        return config.jwtAuthenticationConverter().convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> !authority.startsWith("FACTOR_"))
                .toList();
    }

    private Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token-de-mentira")
                .header("alg", "RS256")
                .claim("sub", "6b3f0c1e-0000-0000-0000-000000000001")
                .claim("preferred_username", "operator");
    }
}
