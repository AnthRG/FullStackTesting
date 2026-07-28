package pucmm.freddy.fullstacktesting.security;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("security")
class AuthorizationMatrixIT extends AbstractSecurityTest {

    private record Usuario(String nombre, Set<String> permisos) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    private record Endpoint(HttpMethod metodo, String path, String permisoRequerido, Supplier<String> cuerpo) {
        @Override
        public String toString() {
            return metodo + " " + path;
        }
    }

    // Los cuerpos son validos a proposito: @Valid se resuelve antes que @PreAuthorize,
    // asi que un cuerpo invalido devolveria 400 y taparia la decision de permisos.
    private static final Supplier<String> SIN_CUERPO = () -> null;

    private static final Supplier<String> PRODUCTO_VALIDO = () -> """
            {"name":"sonda-seguridad","sku":"SEC-%s","description":"alta de prueba",
             "category":"seguridad","price":1.00,"quantity":1,"minimumStock":1,"status":"ACTIVE"}
            """.formatted(UUID.randomUUID());

    private static final Supplier<String> MOVIMIENTO_VALIDO = () -> """
            {"productId":999999,"movementType":"IN","quantity":1,"observations":"sonda"}
            """;

    private static final Set<String> PERMISOS_DE_LA_APP = Set.of(
            "product:view", "product:manage",
            "stock:view", "stock:manage",
            "report:view", "audit:view", "user:manage");

    private static final List<Usuario> USUARIOS = List.of(
            new Usuario("user2", Set.of()),
            new Usuario("user1", Set.of("product:view", "stock:view", "report:view")),
            new Usuario("operator", Set.of("product:view", "product:manage",
                    "stock:view", "stock:manage", "report:view")),
            new Usuario("auditor", Set.of("product:view", "stock:view", "report:view", "audit:view")),
            new Usuario("admin", PERMISOS_DE_LA_APP));

    private static final List<Endpoint> ENDPOINTS = List.of(
            new Endpoint(HttpMethod.GET, "/api/auth/me", null, SIN_CUERPO),
            new Endpoint(HttpMethod.GET, "/api/products", "product:view", SIN_CUERPO),
            new Endpoint(HttpMethod.POST, "/api/products", "product:manage", PRODUCTO_VALIDO),
            new Endpoint(HttpMethod.PUT, "/api/products/999999", "product:manage", PRODUCTO_VALIDO),
            new Endpoint(HttpMethod.DELETE, "/api/products/999999", "product:manage", SIN_CUERPO),
            new Endpoint(HttpMethod.GET, "/api/stock-movements", "stock:view", SIN_CUERPO),
            new Endpoint(HttpMethod.POST, "/api/stock-movements", "stock:manage", MOVIMIENTO_VALIDO),
            new Endpoint(HttpMethod.GET, "/api/notifications", "product:view", SIN_CUERPO),
            new Endpoint(HttpMethod.GET, "/api/reports/summary", "report:view", SIN_CUERPO),
            new Endpoint(HttpMethod.GET, "/api/audit/products", "audit:view", SIN_CUERPO),
            new Endpoint(HttpMethod.GET, "/api/admin/users", "user:manage", SIN_CUERPO));

    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

    static Stream<Arguments> matriz() {
        return USUARIOS.stream()
                .flatMap(usuario -> ENDPOINTS.stream().map(endpoint -> Arguments.of(usuario, endpoint)));
    }

    static Stream<Endpoint> endpoints() {
        return ENDPOINTS.stream();
    }

    static Stream<Usuario> usuarios() {
        return USUARIOS.stream();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("matriz")
    void accesoSegunPermisos(Usuario usuario, Endpoint endpoint) {
        int status = statusOf(endpoint.metodo(), endpoint.path(), token(usuario), endpoint.cuerpo().get());

        if (tienePermiso(usuario, endpoint)) {
            assertThat(status)
                    .as("%s tiene %s y deberia poder %s", usuario, endpoint.permisoRequerido(), endpoint)
                    .isNotIn(401, 403);
        } else {
            assertThat(status)
                    .as("%s no tiene %s y no deberia poder %s", usuario, endpoint.permisoRequerido(), endpoint)
                    .isEqualTo(403);
        }
    }

    @ParameterizedTest(name = "anonimo -> {0}")
    @MethodSource("endpoints")
    void sinTokenTodoEs401(Endpoint endpoint) {
        assertThat(statusOf(endpoint.metodo(), endpoint.path(), null, endpoint.cuerpo().get()))
                .as("%s sin token debe pedir autenticacion, no negar por permisos", endpoint)
                .isEqualTo(401);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("usuarios")
    void elRealmOtorgaExactamenteLosPermisosEsperados(Usuario usuario) throws ParseException {
        Set<String> otorgados = rolesDe(token(usuario)).stream()
                .filter(PERMISOS_DE_LA_APP::contains)
                .collect(Collectors.toSet());

        assertThat(otorgados)
                .as("permisos que el realm expande en el token de %s", usuario)
                .isEqualTo(usuario.permisos());
    }

    private static boolean tienePermiso(Usuario usuario, Endpoint endpoint) {
        return endpoint.permisoRequerido() == null
                || usuario.permisos().contains(endpoint.permisoRequerido());
    }

    private static String token(Usuario usuario) {
        return TOKENS.computeIfAbsent(usuario.nombre(), nombre -> tokenFor(nombre, nombre));
    }

    private static Collection<String> rolesDe(String token) throws ParseException {
        Map<String, Object> realmAccess = SignedJWT.parse(token)
                .getJWTClaimsSet()
                .getJSONObjectClaim("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream().map(Object::toString).toList();
    }
}
