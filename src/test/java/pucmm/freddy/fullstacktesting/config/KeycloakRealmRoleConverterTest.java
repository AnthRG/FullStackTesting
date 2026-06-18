package pucmm.freddy.fullstacktesting.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void convert_conRolesDeRealm_devuelveAuthoritiesConPrefijoRole() {
        Jwt jwt = jwtConClaim("realm_access", Map.of("roles", List.of("VIEW_ROLES", "product:view")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_VIEW_ROLES", "ROLE_product:view");
    }

    @Test
    void convert_sinRealmAccess_devuelveVacio() {
        Jwt jwt = jwtConClaim("scope", "openid");

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void convert_conRealmAccessSinRoles_devuelveVacio() {
        Jwt jwt = jwtConClaim("realm_access", Map.of("otra", "cosa"));

        assertThat(converter.convert(jwt)).isEmpty();
    }

    private Jwt jwtConClaim(String name, Object value) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim(name, value)
                .issuedAt(Instant.EPOCH)
                .expiresAt(Instant.EPOCH.plusSeconds(60))
                .build();
    }
}
