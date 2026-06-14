package pucmm.freddy.fullstacktesting.web;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pucmm.freddy.fullstacktesting.dto.LoginRequest;
import pucmm.freddy.fullstacktesting.dto.LoginResponse;
import pucmm.freddy.fullstacktesting.service.KeycloakAuthService;

/**
 * Endpoints de autenticacion para el frontend.
 *
 * <ul>
 *   <li>{@code POST /api/auth/login} — publico. Cambia credenciales por un token.</li>
 *   <li>{@code GET  /api/auth/me} — protegido. Requiere un JWT valido (lo valida
 *       el Resource Server de Spring Security) y devuelve los datos del usuario.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final KeycloakAuthService auth;

    public AuthController(KeycloakAuthService auth) {
        this.auth = auth;
    }

    /** Login: valida credenciales en Keycloak y devuelve el access token. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.username(), request.password());
    }

    /** Datos del usuario autenticado, leidos del JWT validado por Spring Security. */
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("username", jwt.getClaimAsString("preferred_username"));
        info.put("email", jwt.getClaimAsString("email"));
        info.put("roles", realmRoles(jwt));
        return info;
    }

    private Collection<String> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            return roles.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
