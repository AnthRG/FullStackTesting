package pucmm.freddy.fullstacktesting.web;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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
